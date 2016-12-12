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

import com.devexperts.dlcheck.util.WeakIdentityHashMap;
import com.devexperts.dlcheck.util.WeakIdentityHashSet;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class WeakIdentityHashMapTest {
    private static final int N = 1000;
    private static final int MAX_VALUE = 10_000;
    private static final Object[] OBJECTS = new Object[N];
    private static final Random RANDOM = new Random(0);

    static {
        for (int i = 0; i < N; i++)
            OBJECTS[i] = new Object();
    }

    @Test
    public void testFunctionality() {
        WeakIdentityHashMap<Object, Integer> mapActual = new WeakIdentityHashMap<>();
        Map<Object, Integer> mapExpected = new WeakHashMap<>();
        // Add elements
        for (int i = 0; i < N * 1000; i++) {
            Integer v = RANDOM.nextInt(MAX_VALUE);
            if (RANDOM.nextInt(10) >= 9) {
                // Add element from OBJECTS
                Object o = OBJECTS[RANDOM.nextInt(N)];
                assertEquals(mapExpected.put(o, v), mapActual.put(o, v));
            } else {
                // Add new element
                Object o = new Object();
                mapActual.put(o, v);
                mapExpected.put(o, v);
            }
        }
        // Check
        for (Object o : OBJECTS)
            assertEquals(mapExpected.get(o), mapExpected.get(o));
    }
}
