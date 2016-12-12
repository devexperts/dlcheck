package com.devexperts.dlcheck.util.test;

/*
 * #%L
 * dataintegrator
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

import com.devexperts.dlcheck.util.IntSet;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class IntSetTest {
    private static final int N = 10_000;
    private static final Random RANDOM = new Random(0);

    @Test
    public void testFunctionality() {
        IntSet setActual = new IntSet();
        boolean[] setExpected = new boolean[N];
        // Add elements
        for (int i = 0; i < N * 100; i++) {
            int x = RANDOM.nextInt(N - 1) + 1;
            setActual.add(x);
            setExpected[x] = true;
        }
        // Check
        for (int x = 1; x < N; x++)
            assertEquals(setExpected[x], setActual.contains(x));
    }
}
