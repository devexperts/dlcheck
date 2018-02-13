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


import com.devexperts.dlcheck.api.CycleEdge;
import com.devexperts.dlcheck.util.FastIdentityHashSet;
import com.devexperts.dlcheck.util.IntSet;
import com.devexperts.dlcheck.util.WeakIdentityHashMap;
import com.devexperts.dlcheck.util.WeakIdentityHashSet;

public class LockNode {
    public static final int INITIAL_ORDER = Integer.MAX_VALUE;

    final FastIdentityHashSet<LockNode> children = new FastIdentityHashSet<>();
    final IntSet acquireLocationIds = new IntSet();
    final String desc;
    // Already known edges which create a cycle
    final WeakIdentityHashSet<LockNode> cyclicEdges = new WeakIdentityHashSet<>();
    // Edges from cycles with associated stacktrace
    final WeakIdentityHashMap<LockNode, CycleEdge> edgesFromPublishedCycles = new WeakIdentityHashMap<>();

    volatile int order;
    long visited;

    public LockNode(Object owner) {
        this.desc = owner.getClass().getSimpleName() + "@" + Integer.toString(System.identityHashCode(owner), 16);
        this.order = INITIAL_ORDER;
    }

    void addAcquireLocationId(int acquireLocationId) {
        acquireLocationIds.add(acquireLocationId);
    }

    @Override
    public String toString() {
        return "LockNode{" + desc + ", order=" + order + "}";
    }
}
