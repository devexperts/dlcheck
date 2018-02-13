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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

public class WeakIdentityHashMap<K, V> {
    private static final int MAGIC = 0xB46394CD;
    private static final int INITIAL_SHIFT = 20;
    private static final int THRESHOLD = (int)((1L << 32) * 0.5); // 50% fill factor for speed

    private static final WeakReference EMPTY_KEY = new WeakReference(null);

    private static class Core<K, V> {
        final int shift;
        final int length;
        final AtomicReferenceArray<WeakReference<K>> keys;
        final AtomicReferenceArray<V> values;
        int size;

        Core(int shift) {
            this.shift = shift;
            length = 1 << (32 - shift);
            keys = new AtomicReferenceArray<>(length);
            values = new AtomicReferenceArray<>(length);
        }
    }

    private volatile Core<K, V> core = new Core<>(INITIAL_SHIFT);

    // does not need external synchronization
    public V get(K key) {
        Core<K, V> core = this.core;
        int i = (System.identityHashCode(key) * MAGIC) >>> core.shift;
        while (true) {
            WeakReference<K> k = core.keys.get(i);
            if (k == null)
                return null;
            if (k.get() == key)
                return core.values.get(i);
            if (i == 0)
                i = core.length;
            i--;
        }
    }

    // needs external synchronization
    public V put(K key, V value) {
        V res = addInternal(this.core, key, value);
        if (res == null)
            if (core.size >= (THRESHOLD >>> core.shift))
                rehash();
        return res;
    }

    // needs external synchronization
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V val = get(key);
        if (val == null) {
            val = mappingFunction.apply(key);
            put(key, val);
        }
        return val;
    }

    public synchronized void clean() {
        Core<K, V> core = this.core;
        Lock lock;
        for (int i = 0; i < core.length; i++) {
            WeakReference<K> k = core.keys.get(i);
            if (k != null && k.get() == null) {
                core.keys.set(i, EMPTY_KEY);
                core.values.set(i, null);
            }
        }
    }

    public Iterator<V> values() {
        return new Iterator0();
    }

    private class Iterator0 implements Iterator<V> {
        private final Core<K, V> core;
        private int i;
        private V next;

        Iterator0() {
            this.core = WeakIdentityHashMap.this.core;
            updateIndexToNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public V next() {
            V res = next;
            updateIndexToNext();
            return res;
        }

        @SuppressWarnings("unchecked")
        private void updateIndexToNext() {
            next = null;
            while (i < core.length && next == null) {
                next = core.values.get(i);
                i++;
            }
        }
    }

    private V addInternal(Core<K, V> core, K key, V value) {
        int i = (System.identityHashCode(key) * MAGIC) >>> core.shift;
        int startI = i;
        int firstEmptyKeyIndex = -1;
        while (true) {
            WeakReference<K> k = core.keys.get(i);
            if (k == null) {
                if (firstEmptyKeyIndex != -1) {
                    core.keys.set(firstEmptyKeyIndex, new WeakReference<>(key));
                    core.values.set(firstEmptyKeyIndex, value);
                } else {
                    core.keys.set(i, new WeakReference<>(key));
                    core.values.set(i, value);
                    core.size++;
                }
                if (firstEmptyKeyIndex != -1) {
                    while (true) {
                        if (i == startI)
                            break;
                        i++;
                        if (i == core.length)
                            i = 0;
                        if (core.keys.get(i) == EMPTY_KEY) {
                            core.keys.set(i, null);
                            core.size--;
                        } else {
                            break;
                        }
                    }
                }
                return null;
            } else if (k.get() == key) {
                V oldVal = core.values.get(i);
                core.values.set(i, value);
                return oldVal;
            }
            if (k.get() == null) {
                core.keys.set(i, EMPTY_KEY);
                core.values.set(i, null);
                if (firstEmptyKeyIndex == -1)
                    firstEmptyKeyIndex = i;
            } else if (k == EMPTY_KEY) {
                if (firstEmptyKeyIndex == -1)
                    firstEmptyKeyIndex = i;
            }

            if (i == 0)
                i = core.length;
            i--;
        }
    }

    private void rehash() {
        Core<K, V> oldCore = core;
        Core<K, V> newCore = new Core<>(oldCore.shift - 1);
        for (int i = 0; i < oldCore.length; i++)
            if (oldCore.keys.get(i) != null) {
                K key = oldCore.keys.get(i).get();
                if (key != null) {
                    newCore.size++;
                    addInternal(newCore, key, oldCore.values.get(i));
                }
            }
        core = newCore;
    }
}
