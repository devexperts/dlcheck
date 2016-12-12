package com.devexperts.dlcheck.tests.base;

/*
 * #%L
 * test
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

import com.devexperts.dlcheck.api.CycleNode;
import com.devexperts.dlcheck.api.DlCheckUtils;
import com.devexperts.dlcheck.api.PotentialDeadlockListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class GraphAnalyzerTest {
    private Lock[] locks = new Lock[100];
    private Set<List<String>> expectedCyclesDesc;
    private final PotentialDeadlockListener pdl = potentialDeadlock -> {
        List<String> cycleDesc = potentialDeadlock.getCycle().getNodes().stream()
                .map(CycleNode::getDesc)
                .map(desc -> desc.split("@")[1])
                .collect(Collectors.toList());
        Assert.assertTrue(expectedCyclesDesc.remove(cycleDesc));
    };

    public GraphAnalyzerTest() {
        for (int i = 0; i < locks.length; i++)
            locks[i] = new ReentrantLock();
    }

    @Before
    public void setup() {
        DlCheckUtils.addPotentialDeadlockListener(pdl);
    }

    @After
    public void tearDown() {
        if (expectedCyclesDesc != null)
            Assert.assertTrue(expectedCyclesDesc.isEmpty());
        expectedCyclesDesc = null;
        DlCheckUtils.removePotentialDeadlockListener(pdl);
    }

    @Test
    public void testCycle_2locks() {
        expectedCycles(asList(
                asList(1, 0),
                asList(2, 0),
                asList(2, 1)
        ));
        locks[0].lock(); locks[1].lock(); locks[2].lock();
        unlock(0, 1, 2);
        locks[1].lock(); locks[0].lock();
        unlock(1, 0);
        locks[2].lock(); locks[0].lock();
        unlock(2, 0);
        locks[2].lock(); locks[1].lock();
        unlock(2, 1);
    }

    @Test
    public void testCycle_3_2_locks() {
        expectedCycles(asList(
                asList(1, 0, 3),
                asList(0, 3)
        ));
        locks[0].lock(); locks[1].lock();
        unlock(0, 1);
        locks[3].lock(); locks[0].lock();
        unlock(3, 0);
        locks[1].lock(); locks[2].lock();
        unlock(2, 1);
        locks[1].lock(); locks[3].lock();
        unlock(1, 3);
        locks[3].lock(); locks[0].lock();
        unlock(3, 0);
        locks[0].lock(); locks[3].lock();
        unlock(3, 0);
    }

    private void unlock(int... ids) {
        for (int i : ids)
            locks[i].unlock();
    }

    private void expectedCycles(List<List<Integer>> cycles) {
        expectedCyclesDesc = cycles.stream()
                .map(l -> l.stream()
                        .map(i -> Integer.toString(System.identityHashCode(locks[i]), 16))
                        .collect(Collectors.toList()))
                .collect(Collectors.toSet());
    }

}
