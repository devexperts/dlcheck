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

import com.devexperts.dlcheck.util.FastArrayStack;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.Stack;

import static org.junit.Assert.assertEquals;

public class FastArrayStackTest {
    private static final Random RANDOM = new Random(0);
    private static final int N = 1000;

    @Test
    public void testFunctionality() {
        FastArrayStack<String> stackActual = new FastArrayStack<>();
        Stack<String> stackExpected = new Stack<>();
        for (int i = 0; i < N; i++) {
            if (RANDOM.nextBoolean()) { // Push
                String s = "" + i;
                stackExpected.push(s);
                stackActual.push(s);
            } else { // Pop
                if (stackExpected.size() > 0)
                    assertEquals(stackExpected.pop(), stackActual.pop());
            }
            assertEquals(stackExpected.size(), stackActual.size());
        }
    }

}