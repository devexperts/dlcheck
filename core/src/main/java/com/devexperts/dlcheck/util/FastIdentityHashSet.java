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
import java.util.concurrent.atomic.AtomicReferenceArray;

public class FastIdentityHashSet<T> implements Iterable<T> {

    private static final int MAGIC = 0xB46394CD;
    private static final int MAX_SHIFT = 29;
    private static final int THRESHOLD = (int)((1L << 32) * 0.5); // 50% fill factor for speed

    private static class Core<T> {
        final int shift;
        final int length;
        final AtomicReferenceArray<T> keys;

        Core(int shift) {
            this.shift = shift;
            length = 1 << (32 - shift);
            keys = new AtomicReferenceArray<>(length);
        }
    }

    private volatile Core<T> core = new Core<>(MAX_SHIFT);
    private volatile int size;

    // does not need external synchronization
    public boolean contains(T key) {
        Core<T> core = this.core;
        int i = (System.identityHashCode(key) * MAGIC) >>> core.shift;
        while (true) {
            T curKey = core.keys.get(i);
            if (curKey == null)
                return false;
            if (curKey == key)
                return true;
            if (i == 0)
                i = core.length;
            i--;
        }
    }

    // needs external synchronization
    public boolean add(T key) {
        boolean res = addInternal(this.core, key);
        if (res)
            if (size++ >= (THRESHOLD >>> core.shift))
                rehash();
        return res;
    }

    private boolean addInternal(Core<T> core, T key) {
        int i = (System.identityHashCode(key) * MAGIC) >>> core.shift;
        while (true) {
            T curKey = core.keys.get(i);
            if (curKey == null) {
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
        Core<T> oldCore = core;
        Core<T> newCore = new Core<>(oldCore.shift - 1);
        for (int i = 0; i < oldCore.length; i++) {
            T key = oldCore.keys.get(i);
            if (key != null)
                addInternal(newCore, key);
        }
        core = newCore;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator0();
    }

    private class Iterator0 implements Iterator<T> {

        private final Core<T> core;
        private int i;
        private T next;

        Iterator0() {
            this.core = FastIdentityHashSet.this.core;
            updateIndexToNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            T res = next;
            updateIndexToNext();
            return res;
        }

        @SuppressWarnings("unchecked")
        private void updateIndexToNext() {
            next = null;
            while (i < core.length && next == null) {
                if (core.keys.get(i) != null)
                    next = core.keys.get(i);
                i++;
            }
        }
    }
}
