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

import com.devexperts.dlcheck.util.FastIdentityHashSet;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import static org.junit.Assert.assertEquals;

public class FastIdentityHashSetTest {
    private static final int N = 10_000;
    private static final Object[] OBJECTS = new Object[N];
    private static final Random RANDOM = new Random(0);

    static {
        for (int i = 0; i < N; i++)
            OBJECTS[i] = new Object();
    }

    @Test
    public void testFunctionality() {
        FastIdentityHashSet<Object> setActual = new FastIdentityHashSet<>();
        Set<Object> setExpected = Collections.newSetFromMap(new WeakHashMap<>());
        // Add elements
        for (int i = 0; i < N * 100; i++) {
            Object o;
            if (RANDOM.nextBoolean()) {
                // Add element from OBJECTS
                o = OBJECTS[RANDOM.nextInt(N)];
            } else {
                // Add new element
                o = new Object();
            }
            setActual.add(o);
            setExpected.add(o);
        }
        // Check
        for (Object o : OBJECTS)
            assertEquals(setExpected.contains(o), setActual.contains(o));
    }
}