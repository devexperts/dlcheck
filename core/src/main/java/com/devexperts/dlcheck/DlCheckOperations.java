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

import com.devexperts.dlcheck.util.LockNodeStack;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DlCheckOperations {
    // Lock graph GC period
    private static final long STATS_PERIOD =
            Long.parseLong(System.getProperty("dlcheck.stats.period", "60000"));
    // Statistics counter
    private static final Stats stats = new Stats();
    // Stores lock graph based as standard wait-for graph, but without edges removing
    private static final LockGraph lockGraph = new LockGraph(stats);
    // Lock stack for current thread
    private static final ThreadLocal<LockNodeStack> lockStacks = new ThreadLocal<LockNodeStack>() {
        @Override
        protected LockNodeStack initialValue() {
            return new LockNodeStack();
        }
    };

    static {
        if (Stats.STATS_ENABLED) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                    stats.dump();
                    lockGraph.dump();
                }
            }, STATS_PERIOD, STATS_PERIOD);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void afterMonitorEnter(Object owner, int locationId) {
        try {
            if (owner instanceof ReentrantReadWriteLock.ReadLock)
                return;
            long startTime = 0;
            if (Stats.STATS_ENABLED) {
                stats.inc_monitor_enters();
                startTime = System.nanoTime();
                if (owner instanceof LockNodeHolder)
                    stats.inc_monitor_enters_on_lock_node_holder();
                else if (owner.getClass() == Object.class)
                    stats.inc_monitor_enters_on_object();
                else if (owner instanceof Lock)
                    stats.inc_monitor_enters_on_lock();
                else
                    stats.inc_monitor_enters_on_custom_object();
            }
            LockNodeStack lockStack = lockStacks.get();
            LockNode node = getLockNode(owner);
            node.addAcquireLocationId(locationId);
            long addEdgesStartTime = 0;
            if (Stats.STATS_ENABLED)
                addEdgesStartTime = System.nanoTime();
            lockGraph.addEdges(node, lockStack);
            lockStack.push(node);
            if (Stats.STATS_ENABLED) {
                long endTime = System.nanoTime();
                stats.increase_monitor_enters_time(endTime - startTime);
                stats.increase_gets_owners_time(addEdgesStartTime - startTime);
                stats.increase_adds_adges_time(endTime - addEdgesStartTime);
            }
        } catch (Throwable t) {
            if (t instanceof AssertionError)
                throw t;
            t.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void beforeMonitorExit(Object owner) {
        try {
            LockNodeStack lockStack = lockStacks.get();
            LockNode node = getLockNode(owner);
            if (lockStack.remove(node))
                stats.inc_monitor_exits_from_top();
        } catch (Throwable t) {
            if (t instanceof AssertionError)
                throw t;
            t.printStackTrace();
        }
    }

    private static LockNode getLockNode(Object owner) {
        if (owner instanceof LockNodeHolder) {
            LockNode node = ((LockNodeHolder) owner).__dlcheck_get_lock_node__();
            if (node == null) {
                node = lockGraph.createNode(owner);
                ((LockNodeHolder) owner).__dlcheck_set_lock_node__(node);
            }
            return node;
        } else {
            return lockGraph.getNode(owner);
        }
    }
}