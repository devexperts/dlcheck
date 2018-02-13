package com.devexperts.dlcheck.tests.base;
/*
* #%L
 * * test
 * *
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
 * *
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
import com.devexperts.dlcheck.api.xml.io.PotentialDeadlockMarshaller;
import com.devexperts.dlcheck.api.xml.io.WatchTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnmarshallingTest {
    private final PotentialDeadlockListener PDL = new PotentialDeadlockListener() {
        @Override
        public void onPotentialDeadlock(PotentialDeadlock potentialDeadlock) {
            marshalled.add(potentialDeadlock);
        }

        @Override
        public void onFoundStackTrace(CycleEdge cycleEdge) {
            marshalled.add(cycleEdge);
        }
    };
    // Contains CycleNodes and CycleEdges, can't add PotentialDeadlocks
    // because their cycles are changing during the process of unmarshalling
    private HashSet<Object> marshalled = new HashSet<>();
    private HashSet<Object> unmarshalled = new HashSet<>();

    @Before
    public void setUp() {
        DlCheckUtils.addPotentialDeadlockListener(PDL);
    }

    @After
    public void tearDown() {
        DlCheckUtils.removePotentialDeadlockListener(PDL);
    }

    @Test
    public void test() {
        Thread thread = new Thread(() -> {
            // Wait for dlcheck to recreate xml output
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            try {
                Path path = Paths.get(System.getProperty(PotentialDeadlockMarshaller.XML_OUT_PROPERTY));
                WatchTask watchTask = new WatchTask(path.toFile());
                // Count unmarshalled objects
                watchTask.setPotentialDeadlockHandler(pd -> unmarshalled.add(pd));
                watchTask.setNewStackTraceHandler(edge -> unmarshalled.add(edge));
                while (true) {
                    // When dlcheck tag closed
                    if (watchTask.isFinish()) break;
                    watchTask.tryRead();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        thread.start();
        makeLockHierarchyViolations();
        try {
            thread.join(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        // Output is also containing data from previous tests
        assertTrue(unmarshalled.containsAll(marshalled));
    }

    private void makeLockHierarchyViolations() {
        Lock[] x = new Lock[4];
        for (int i = 0; i < x.length; ++i) {
            x[i] = new ReentrantLock();
        }
        for (int j = 1; j < x.length; ++j) {
            // Add chain in graph
            EdgesStackTracesTest.lockAndUnlockAdjacent(x);
            x[j].lock();
            x[0].lock();
            x[0].unlock();
            x[j].unlock();
        }
    }
}
