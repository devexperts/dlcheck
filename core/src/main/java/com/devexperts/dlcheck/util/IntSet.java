package com.devexperts.dlcheck.util;

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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class IntSet implements Iterable<Integer> {

    private static final int MAGIC = 0xB46394CD;
    private static final int MAX_SHIFT = 29;
    private static final int THRESHOLD = (int)((1L << 32) * 0.5); // 50% fill factor for speed
    private static final int NAN = 0;

    private static class Core {
        final int shift;
        final int length;
        final AtomicIntegerArray keys;

        Core(int shift) {
            this.shift = shift;
            length = 1 << (32 - shift);
            keys = new AtomicIntegerArray(length);
        }
    }

    private volatile Core core = new Core(MAX_SHIFT);
    private volatile int size;

    // does not need external synchronization
    public boolean contains(int key) {
        Core core = this.core;
        int i = (key * MAGIC) >>> core.shift;
        while (true) {
            int curKey = core.keys.get(i);
            if (curKey == NAN)
                return false;
            if (curKey == key)
                return true;
            if (i == 0)
                i = core.length;
            i--;
        }
    }

    public boolean containsAll(IntSet set) {
        Core core = set.core;
        for (int i = 0; i < core.length; i++) {
            int x = core.keys.get(i);
            if (x != NAN && !contains(x))
                return false;
        }
        return true;
    }

    // needs external synchronization
    public void add(int key) {
        if (addInternal(this.core, key))
            if (++size >= (THRESHOLD >>> core.shift))
                rehash();
    }

    private boolean addInternal(Core core, int key) {
        int i = (key * MAGIC) >>> core.shift;
        while (true) {
            int curKey = core.keys.get(i);
            if (curKey == NAN) {
                core.keys.set(i, key);
                return true;
            } else if (curKey == key) {
                return false;
            }
            if (i == 0)
                i = core.length;
            i--;
        }
    }

    private void rehash() {
        Core oldCore = core;
        Core newCore = new Core(oldCore.shift - 1);
        for (int i = 0; i < oldCore.length; i++) {
            int key = oldCore.keys.get(i);
            if (key != NAN)
                addInternal(newCore, key);
        }
        core = newCore;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator0();
    }

    private class Iterator0 implements Iterator<Integer> {

        private final Core core;
        private int i;
        private int next = NAN;

        Iterator0() {
            this.core = IntSet.this.core;
            updateIndexToNext();
        }

        @Override
        public boolean hasNext() {
            return next != NAN;
        }

        @Override
        public Integer next() {
            int res = next;
            updateIndexToNext();
            return res;
        }

        @SuppressWarnings("unchecked")
        private void updateIndexToNext() {
            next = NAN;
            while (i < core.length && next == NAN) {
                next = core.keys.get(i);
                i++;
            }
        }
    }
}
