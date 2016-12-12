package com.devexperts.dlcheck.util.test;

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

import com.devexperts.dlcheck.LockNode;
import com.devexperts.dlcheck.util.LockNodeStack;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

@Ignore
public class LockNodeStackTest {
    private static final int N = 10000;
    private static final LockNode[] NODES = new LockNode[N];
    private static final Random RANDOM = new Random(42);

    static {
        for (int i = 0; i < NODES.length; i++) {
            NODES[i] = new LockNode(new Object());
        }
    }

    @Test
    public void testOperations() {
        LockNodeStack stack = new LockNodeStack();
        Map<LockNode, Integer> mapAsStack = new HashMap<>();
        int expectedSize = 0;
        for (int i = 0; i < N * 10; i++) {
            LockNode node = NODES[RANDOM.nextInt(N)];
            if (RANDOM.nextBoolean()) {
                // Push to stack
                expectedSize++;
                // Push to map
                stack.push(node);
                Integer count = mapAsStack.get(node);
                if (count == null)
                    count = 0;
                count++;
                mapAsStack.put(node, count);
            } else {
                // Remove from stack
                boolean removeFromStack = stack.remove(node);
                // Remove from map
                boolean removeFromMap = false;
                if (mapAsStack.containsKey(node)) {
                    removeFromMap = true;
                    expectedSize--;
                    int count = mapAsStack.remove(node);
                    if (count > 1)
                        mapAsStack.put(node, count - 1);
                }
                // Check
                assertEquals(removeFromMap, removeFromStack);
            }
            // Check size
            assertEquals(expectedSize, stack.size());
        }
    }
}
