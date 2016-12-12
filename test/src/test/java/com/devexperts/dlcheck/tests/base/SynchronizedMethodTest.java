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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SynchronizedMethodTest extends LockTestBase {

    @Test
    public void test() {
        B.a.f();
        B.g();

        assertEquals(1, potentialDeadlocksCount);
    }

    @Test
    public void testWithException() {
        // Build edge "l" -> "B"
        Object l = new Object();
        synchronized (l) {
            A.f4();
        }
        // Build edge "B" -> "B"
        try {
            A.f3();
        } catch (Exception e) {}
        // Then lock stack should be empty.
        // Test it, just acquire "l"
        synchronized (l) {}
        // Check that no potential deadlocks detected
        assertEquals(0, potentialDeadlocksCount);
    }

    @Test
    public void testWithLoop() {
        // Build edge "l" -> "B"
        Object l = new Object();
        synchronized (l) {
            A.f4();
        }
        // Then lock stack should be empty.
        // Test it, just acquire "l"
        synchronized (l) {}
        // Check that no potential deadlocks detected
        assertEquals(0, potentialDeadlocksCount);
    }

    static class A {
        final synchronized void f() {
            B.g2();
        }

        synchronized void f2() {
        }

        static synchronized void f3() {
            B.g3();
        }

        static int x4;

        static synchronized void f4() {
            while (x4 < 10) { x4++; }
        }
    }

    static class B {
        static final A a = new A();

        static synchronized void g() {
            a.f2();
        }

        static synchronized void g2() {
        }

        static synchronized void g3() {
            throw new RuntimeException();
        }
    }
}
