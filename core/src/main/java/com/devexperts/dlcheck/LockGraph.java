package com.devexperts.dlcheck;

/*
 * #%L
 * core
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.devexperts.dlcheck.api.Cycle;
import com.devexperts.dlcheck.api.CycleNode;
import com.devexperts.dlcheck.api.DlCheckUtils;
import com.devexperts.dlcheck.api.PotentialDeadlock;
import com.devexperts.dlcheck.util.ConcurrentWeakIdentityHashMap;
import com.devexperts.dlcheck.util.FastArrayStack;
import com.devexperts.dlcheck.util.FastIdentityHashSet;
import com.devexperts.dlcheck.util.FastObjArrayList;
import com.devexperts.dlcheck.util.LockNodeStack;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

class LockGraph {
    private static final int MAX_GC_COLLECTED_LOCK_NODES = 1000;
    private static final int MAX_BUFFER_SIZE = 1024;
    private static final Set<List<LockNode>> FOUND_CYCLES = new HashSet<>();
    private static final Set<PotentialDeadlock> FOUND_DEADLOCKS = new HashSet<>();

    private final ConcurrentWeakIdentityHashMap<Object, LockNode> nodes = new ConcurrentWeakIdentityHashMap<>(1_000, 0.5f);

    private final Stats stats;

    private final ReadWriteLock graphLock = new ReentrantReadWriteLock();
    private final ThreadLocal<FastObjArrayList<LockNode>> newEdgesHolder = new ThreadLocal<FastObjArrayList<LockNode>>() {
        @Override
        protected FastObjArrayList<LockNode> initialValue() {
            return new FastObjArrayList<>();
        }
    };

    private final FastObjArrayList<WeakReference<LockNode>> ordInv = new FastObjArrayList<>();
    private final FastArrayStack<LockNode> stack = new FastArrayStack<>();

    private ConcurrentLinkedQueue<LockNode> newLockNodeBuffer = new ConcurrentLinkedQueue<>();
    private ReferenceQueue<LockNode> refQueue = new ReferenceQueue<>();
    private int numberOfCollectedByGCLockNodes = 0;

    LockGraph(Stats stats) {
        this.stats = stats;
    }

    // Need to be synchronized on "owner"
    LockNode getNode(Object owner) {
        return nodes.computeIfAbsent(owner, this::createNode);
    }

    public LockNode createNode(Object owner) {
        // Maintain statistics
        long startTime = 0;
        if (Stats.STATS_ENABLED) {
            startTime = System.nanoTime();
            stats.inc_new_lock_nodes();
        }
        // Create new LockNode for specified owner and try to push it to temporary buffer
        LockNode lockNode = new LockNode(owner);
        newLockNodeBuffer.offer(lockNode);
        if (newLockNodeBuffer.size() > MAX_BUFFER_SIZE) {
            // Maintain statistics
            long startTime_cleanUpBuffer = 0;
            if (Stats.STATS_ENABLED) {
                startTime_cleanUpBuffer = System.nanoTime();
                stats.inc_cleans_up_buffer_from_creae_node();
            }
            // Buffer is full, need to clean up.
            graphLock.writeLock().lock();
            try {
                // Compress ordInv if needed.
                // It's important to compress it not only into maintaining topological order invocation,
                // because it's possible to write code where are a lot of short-time lived locks and
                // no edges will be created. In such case ordInv will contain empty WeakReference-s
                // memory leak will be occurred.
                compressOrdInvIfNeeded();
                // Double-check under lock (may be cleaned up by another thread)
                flushNewLockNodeBuffer();
            } finally {
                graphLock.writeLock().unlock();
            }
            // Maintain statistics
            if (Stats.STATS_ENABLED)
                stats.increase_cleans_up_buffer_from_creae_node(System.nanoTime() - startTime_cleanUpBuffer);
        }
        // Maintain statistics
        if (Stats.STATS_ENABLED) {
            stats.increase_new_lock_nodes_time(System.nanoTime() - startTime);
        }
        return lockNode;
    }

    // guarded by "lockGraph.writeLock()"
    private void flushNewLockNodeBuffer() {
        LockNode lockNode;
        while ((lockNode = newLockNodeBuffer.poll()) != null) {
            // Add new LockNode to the beginning of current topological order
            lockNode.order = ordInv.size();
            ordInv.add(new WeakReference<>(lockNode, refQueue));
        }
    }


    // guarded by "lockGraph.writeLock()"
    private void compressOrdInv() {
        int i = 0;
        for (int j = 0; j < ordInv.size(); j++) {
            WeakReference<LockNode> r = ordInv.get(j);
            LockNode node = r.get();
            if (node != null) {
                int order = i++;
                ordInv.put(order, r);
                node.order = order;
            }
        }
        ordInv.shrink(i);
    }

    // guarded by "lockGraph.writeLock()"
    private void compressOrdInvIfNeeded() {
        // Note that refQueue may return non-null value
        // even if compressOrdInv() was called a moment ago.
        // But here is no relationship which can allows to prove
        // that numberOfCollectedByGCLockNodes is a number of
        // LockNode-s in ordInv which are collected by GC.
        // Therefore compressOrdInv() may compress nothing.
        while (refQueue.poll() != null)
            numberOfCollectedByGCLockNodes++;
        if (numberOfCollectedByGCLockNodes > MAX_GC_COLLECTED_LOCK_NODES) {
            numberOfCollectedByGCLockNodes = 0;
            compressOrdInv();
        }
    }

    void addEdges(LockNode from, LockNodeStack lockStack) {
                    stats.inc_lock_stack_size_usage(lockStack.size());
        // Check that lockStack size is not empty
        if (lockStack.size() == 0)
            return;
        // Check that current lock acquiring isn't reentrant
        for (LockNode to : lockStack) {
            if (from == to) {
                // Maintain statistics
                stats.inc_monitor_enters_reentrant();
                return;
            }
        }
        // Do not flush and compress several times
        boolean flushedAndCompressed = false;
        // Detect new edges
        FastObjArrayList<LockNode> newEdges = newEdgesHolder.get();
        for (LockNode to : lockStack) {
            // Flush buffer with lock nodes if order of edge end doesn't defined already
            if (to.order == LockNode.INITIAL_ORDER) {
                graphLock.writeLock().lock();
                try {
                    compressOrdInv();
                    flushNewLockNodeBuffer();
                } finally {
                    graphLock.writeLock().unlock();
                }
                flushedAndCompressed = true;
            }
            // Check that edge 'from' -> 'to' isn't already known
            if (!from.children.contains(to) && !from.cyclicEdges.contains(to))
                newEdges.add(to);
        }
        // Do nothing if all edges are already known
        if (newEdges.size() == 0)
            return;
        // Check that all new edges could be added
        // without maintaining topological order
        graphLock.readLock().lock();
        try {
            LockNode rightmost = null;
            for (int i = 0; i < newEdges.size(); i++) {
                LockNode to = newEdges.get(i);
                if (rightmost == null || to.order > rightmost.order)
                    rightmost = to;
            }
            if (from.order > rightmost.order) {
                // All new edges could be added
                // according to current topological order
                for (LockNode to : lockStack) {
                    if (from.children.add(to)) {
                        stats.inc_edge_adds_new();
                    } else {
                        stats.inc_edge_adds_same();
                    }
                }
                newEdges.clear();
                return;
            }

        } finally {
            graphLock.readLock().unlock();
        }
        // Need to maintain topological order
        // Maintain statistics
        long startTime_maintainTopologicalOrder =  0;
        if (Stats.STATS_ENABLED) {
            startTime_maintainTopologicalOrder = System.nanoTime();
            stats.inc_maintains_top_order();
        }
        List<List<LockNode>> cycles = null;
        graphLock.writeLock().lock();
        try {
            cycles = maintainTopologicalOrderAndGetCycle(from, newEdges, flushedAndCompressed);
        } finally {
            graphLock.writeLock().unlock();
        }
        newEdges.clear();
        if (cycles != null) {
            for (List<LockNode> cycle : cycles)
                publishPotentialDeadlockIfNeeded(cycle, lockStack);
        }
        // Maintain statistics
        if (Stats.STATS_ENABLED)
            stats.increase_maintains_top_order_time(System.nanoTime() - startTime_maintainTopologicalOrder);
    }

    private List<List<LockNode>> maintainTopologicalOrderAndGetCycle(LockNode from,
        FastObjArrayList<LockNode> newEdges, boolean flushedAndCompressed)
    {
        // Find lock node with rightmost order
        // under writeLock.
        LockNode rightmost = null;
        for (int i = 0; i < newEdges.size(); i++) {
            LockNode to = newEdges.get(i);
            if (rightmost == null || to.order > rightmost.order)
                rightmost = to;
        }
        // Flush new lock nodes buffer and compress ordInv if needed if this is not done before
        if (!flushedAndCompressed) {
            compressOrdInvIfNeeded();
            flushNewLockNodeBuffer();
        }
        // Maintain topological order
        if (maintainTopologicalOrder(from, rightmost)) {
            // All new edges could be added
            // according to new topological order
            for (int i = 0; i < newEdges.size(); i++) {
                LockNode to = newEdges.get(i);
                if (from.children.add(to)) {
                    stats.inc_edge_adds_new();
                } else {
                    stats.inc_edge_adds_same();
                }
            }
            return null;
        }
        // Topological order not maintained
        // Find cycle if only one new edge tried to be added
        if (newEdges.size() == 1) {
            LockNode to = newEdges.get(0);
            // Edge 'from' -> 'to' is cyclic!
            // Add this edge as cyclic, find cycle
            // and publish potential deadlock if needed
            from.cyclicEdges.add(to);
            List<LockNode> cyclePath = shortestPath(to, from);
            // Maintain statistics
            stats.inc_cycles();
            return Collections.singletonList(cyclePath);
        }
        List<List<LockNode>> cycles = new ArrayList<>();
        for (int i = 0; i < newEdges.size(); i++) {
            LockNode to = newEdges.get(i);
            if (maintainTopologicalOrder(from, to)) {
                // Add edge 'from' -> 'edge'
                from.children.add(to);
                // Maintain statistics
                stats.inc_edge_adds_new();
            } else {
                // Edge 'from' -> 'to' is cyclic!
                // Add this edge as cyclic, find cycle
                // and publish potential deadlock if needed
                from.cyclicEdges.add(to);
                cycles.add(shortestPath(to, from));
                // Maintain statistics
                stats.inc_cycles();
            }
        }
        return cycles;
    }

    private boolean maintainTopologicalOrder(LockNode from, LockNode to) {
        if (from.order > to.order)
            return true;
        FastArrayStack<LockNode> stack = this.stack;
        int lb = from.order;
        int ub = to.order;
        newVisited();
        stack.push(to);
        while (stack.size() > 0) {
            LockNode u = stack.pop();
            visit(u);
            for (LockNode v : u.children) {
                if (v == from) {
                    return false;
                }
                if (!visited(v) && v.order > lb) {
                    stack.push(v);
                }
            }
        }
        stack.clear();
        ArrayList<LockNode> l = new ArrayList<>();
        int shift = 0;
        int i = ub;
        for (; i >= lb; i--) {
            LockNode w = ordInv.get(i).get();
            if (w == null) {
                continue;
            }
            if (visited(w)) {
                l.add(w);
                shift++;
            } else {
                allocate(w, i + shift);
            }
        }
        for (LockNode node : l) {
            allocate(node, i + shift);
            i--;
        }
        return true;
    }

    private void allocate(LockNode lockNode, int order) {
        lockNode.order = order;
        ordInv.put(order, new WeakReference<>(lockNode, refQueue)); // TODO do not create weak reference
    }

    private long visitedCounter;
    private void newVisited() {
        visitedCounter++;
    }

    private void visit(LockNode lockNode) {
        lockNode.visited = visitedCounter;
    }

    private boolean visited(LockNode lockNode) {
        return lockNode.visited == visitedCounter;
    }

    private void publishPotentialDeadlockIfNeeded(List<LockNode> cyclePath, LockNodeStack lockStack) {
        for (List<LockNode> c : FOUND_CYCLES) {
            if (cyclePath.size() != c.size())
                continue;
            boolean flag = true;
            for (Iterator<LockNode> newIt = cyclePath.iterator(), oldIt = c.iterator(); newIt.hasNext() && oldIt.hasNext(); ) {
                LockNode newNode = newIt.next();
                LockNode oldNode = oldIt.next();
                if (!newNode.acquireLocationIds.containsAll(oldNode.acquireLocationIds)) {
                    flag = false;
                    break;
                }
            }
            if (flag)
                return;
        }
        FOUND_CYCLES.add(cyclePath);
        stats.inc_cycles_unique();
        Cycle cycle = new Cycle(cyclePath.stream().map(
                lockNode -> new CycleNode(lockNode.desc, getLocations(lockNode.acquireLocationIds))
        ).collect(Collectors.toList()));
        List<CycleNode> lockStackElements = Arrays.stream(lockStack.cloneAsArray()).map(
                lockNode -> new CycleNode(lockNode.desc, getLocations(lockNode.acquireLocationIds))
        ).collect(Collectors.toList());
        PotentialDeadlock pd = PotentialDeadlock.create(cycle, lockStackElements);
        if (FOUND_DEADLOCKS.add(pd)) {
            stats.inc_potential_deadlocks();
            DlCheckUtils.notifyAboutPotentialDeadlock(pd);
        }
    }

    private Set<StackTraceElement> getLocations(Iterable<Integer> set) {
        Set<StackTraceElement> ls = new HashSet<>();
        for (int x : set)
            ls.add(LocationManager.getInstance().getLocation(x));
        return ls;
    }

    private List<LockNode> shortestPath(LockNode from, LockNode to) {
        FastIdentityHashSet<LockNode> visited = new FastIdentityHashSet<>();
        ArrayDeque<LockNode> dequeue = new ArrayDeque<>();
        Map<LockNode, LockNode> parent = new HashMap<>();
        dequeue.offer(from);
        while (!dequeue.isEmpty()) {
            LockNode u = dequeue.poll();
            for (LockNode v : u.children) {
                if (visited.add(v)) {
                    dequeue.offer(v);
                    visited.add(v);
                    parent.put(v, u);
                }
                if (v == to) {
                    List<LockNode> path = new ArrayList<>();
                    path.add(to);
                    LockNode cur = to;
                    while (parent.get(cur) != null) {
                        cur = parent.get(cur);
                        path.add(cur);
                    }
                    Collections.reverse(path);
                    return path;
                }
            }
        }
        return new ArrayList<>();
    }

    void dump() {
        try (PrintWriter pw = new PrintWriter(Stats.createStatsFilePath(Stats.GRAPH_DUMP_EXTENSION).toFile())) {
            pw.println("digraph LockGraph {");
            FastIdentityHashSet<LockNode> visited = new FastIdentityHashSet<>();
            for (LockNode node : nodes.values()) {
                if (visited.contains(node))
                    continue;
                Queue<LockNode> q = new ArrayDeque<>();
                visited.add(node);
                q.add(node);
                while (!q.isEmpty()) {
                    LockNode u = q.poll();
                    for (LockNode v : u.children) {
                        pw.println("  \"" + u.desc + "\" -> \"" + v.desc + "\"");
                        if (visited.add(v))
                            q.add(v);
                    }
                    for (LockNode v : u.cyclicEdges) {
                        pw.println("  \"" + u.desc + "\" -> \"" + v.desc + "\" [color=yellow]");
                        if (visited.add(v))
                            q.add(v);
                    }
                }
            }
            pw.println("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
