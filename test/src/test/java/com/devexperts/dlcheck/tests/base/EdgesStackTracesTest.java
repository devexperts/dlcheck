package com.devexperts.dlcheck.tests.base;

/*
 * #%L
 * test
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
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

import com.devexperts.dlcheck.api.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;

public class EdgesStackTracesTest {
    private int unknownEdgesStackTrace;
    private int knownEdgesStackTrace;

    private PotentialDeadlockListener PDL = new PotentialDeadlockListener() {
        @Override
        public void onPotentialDeadlock(PotentialDeadlock potentialDeadlock) {
            Cycle cycle = potentialDeadlock.getCycle();
            int edgesWithStackTrace = 0, edgesWithoutStackTrace = 0;
            for (CycleEdge edge : cycle.getEdges()) {
                if (edge.getStackTrace() != null) {
                    edgesWithStackTrace++;
                } else {
                    edgesWithoutStackTrace++;
                }
            }
            knownEdgesStackTrace = edgesWithStackTrace;
            unknownEdgesStackTrace = edgesWithoutStackTrace;
        }
    };

    @Before
    public void setUp() {
        DlCheckUtils.addPotentialDeadlockListener(PDL);
    }

    @After
    public void tearDown() {
        DlCheckUtils.removePotentialDeadlockListener(PDL);
    }

    @Test
    public void simpleTest() {
        Lock[] x = new Lock[4];
        for (int i = 0; i < x.length; ++i) {
            x[i] = new ReentrantLock();
        }
        for (int j = 1; j < x.length; ++j) {
            // Add chain in graph
            lockAndUnlockAdjacent(x);
            x[j].lock();
            x[0].lock();
            x[0].unlock();
            x[j].unlock();

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            // Acquired stacktrace of newly found edge lying of cycle on previous step
            assertEquals(knownEdgesStackTrace, j);
            // Edge x[j-1] <- x[j] found as cyclic on current step
            assertEquals(unknownEdgesStackTrace, 1);
        }
    }

    static void lockAndUnlockAdjacent(Lock[] x) {
        for (int i = 0; i < x.length - 1; ++i) {
            x[i].lock();
            x[i + 1].lock();
            x[i + 1].unlock();
            x[i].unlock();
        }
    }

    @Test
    public void anotherSimpleTest() {
        Lock[] x = new Lock[10];
        for (int i = 0; i < x.length; ++i) {
            x[i] = new ReentrantLock();
        }
        for (int j = 0; j < x.length - 1; ++j) {
            // Add chain in LockGraph
            lockAndUnlockAdjacent(x);
            x[x.length - 1 - j].lock();
            x[0].lock();
            x[0].unlock();
            x[x.length - 1 - j].unlock();
            // After first step all edges of chain is already known
            assertEquals(knownEdgesStackTrace, j == 0 ? 1 : x.length - j);
            assertEquals(unknownEdgesStackTrace, j == 0 ? x.length - 1 : 0);
        }
    }
}
