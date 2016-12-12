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

public class SynchronizedStatementTest extends LockTestBase {

    @Test
    public void test() {
        Object l1 = new Object();
        Object l2 = new Object();

        synchronized (l1) {
            synchronized (l2) {
            }
        }
        synchronized (l2) {
            synchronized (l1) {
            }
        }

        assertEquals(1, potentialDeadlocksCount);
    }

    @Test
    public void testWithException() {
        // Build edge a -> mf
        B a = new B();
        synchronized (a) {
            a.f2();
        }
        // Build edge mf -> mg
        try {
            a.f();
        } catch (Exception e) {}
        // Then lock stack should be empty.
        // Test it, just acquire "l"
        synchronized (a) {}
        // Check that no potential deadlocks detected
        assertEquals(0, potentialDeadlocksCount);

        assertEquals(0, potentialDeadlocksCount);
    }

    static class B {
        Object mf = new Object(), mg = new Object();

        void f() {
            synchronized (mf) {
                g();
            }
        }

        void f2() {
            synchronized (mf) {
            }
        }

        void g() {
            synchronized (mg) {
                throw new RuntimeException();
            }
        }
    }
}
