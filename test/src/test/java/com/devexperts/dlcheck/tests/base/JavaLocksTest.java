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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertEquals;

public class JavaLocksTest extends LockTestBase {

    int x;
    Lock l1 = new ReentrantLock();

    @Test
    public void test() throws InterruptedException {
        ReadWriteLock l2 = new ReentrantReadWriteLock();

        l1.lock();
        l2.writeLock().lock();
        l2.writeLock().unlock();
        l1.unlock();

        l2.writeLock().lock();
        l1.lock();
        l1.unlock();
        l2.writeLock().unlock();

        assertEquals(1, potentialDeadlocksCount);
    }


    static Lock[] l3 = {new ReentrantLock()};

    @Test
    public void testTryLock() throws InterruptedException {
        Lock l2 = new ReentrantLock();

        l3[0].lock();
        try {
            while (!l2.tryLock(100, TimeUnit.MILLISECONDS)) ;
            try {
                x = 5;
            } finally {
                l2.unlock();
            }
        } finally {
            l3[0].unlock();
        }

        l2.tryLock();
        l3[0].tryLock();
        l3[0].unlock();
        l2.unlock();

        assertEquals(1, potentialDeadlocksCount);
    }

}
