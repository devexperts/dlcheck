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

public class LongObjWeakValuesHashMap<V> {
    private static final int MAGIC = 0xB46394CD;
    private static final int INITIAL_SHIFT = 20;
    private static final int THRESHOLD = (int)((1L << 32) * 0.5); // 50% fill factor for speed

    private static final long EMPTY_KEY = Long.MAX_VALUE;
    private static final long NULL_KEY = 0;

    private static class Core<V> {
        final int shift;
        final int length;
        final long[] keys;
        final WeakReference<V>[] values;
        int size;

        Core(int shift) {
            this.shift = shift;
            length = 1 << (32 - shift);
            keys = new long[length];
            values = new WeakReference[length];
        }
    }

    private volatile Core<V> core = new Core<>(INITIAL_SHIFT);

    public V get(long key) {
        Core<V> core = this.core;
        int i = hash(key) >>> core.shift;
        while (true) {
            long k = core.keys[i];
            if (k == NULL_KEY)
                return null;
            if (k == key)
                return core.values[i].get();
            if (i == 0)
                i = core.length;
            i--;
        }
    }

    public V put(long key, V value) {
        V res = addInternal(this.core, key, value);
        if (res == null)
            if (core.size >= (THRESHOLD >>> core.shift))
                rehash();
        return res;
    }

    private int hash(long k) {
        return ((int)k ^ (int)(k >>> 32)) * MAGIC;
    }

    private V addInternal(Core<V> core, long key, V value) {
        int i = hash(key) >>> core.shift;
        int startI = i;
        int firstEmptyKeyIndex = -1;
        while (true) {
            long k = core.keys[i];
            WeakReference<V> vr = core.values[i];
            if (k == NULL_KEY) {
                if (firstEmptyKeyIndex != -1) {
                    core.keys[firstEmptyKeyIndex] = key;
                    core.values[firstEmptyKeyIndex] = new WeakReference<V>(value);
                    while (true) {
                        if (i == startI)
                            break;
                        i++;
                        if (i == core.length)
                            i = 0;
                        if (core.keys[i] == EMPTY_KEY) {
                            core.keys[i] = NULL_KEY;
                            core.size--;
                        } else {
                            break;
                        }
                    }
                } else {
                    core.keys[i] = key;
                    core.values[i] = new WeakReference<V>(value);
                    core.size++;
                }
                return null;
            } else if (k == EMPTY_KEY) {
                if (firstEmptyKeyIndex == -1)
                    firstEmptyKeyIndex = i;
            } else if (k == key) {
                V oldVal = vr.get();
                if (value != oldVal)
                    core.values[i] = new WeakReference<>(value);
                return oldVal;
            } else if (vr.get() == null) {
                core.keys[i] = EMPTY_KEY;
                core.values[i] = null;
                if (firstEmptyKeyIndex == -1)
                    firstEmptyKeyIndex = i;
            }

            if (i == 0)
                i = core.length;
            i--;
        }
    }

    private void rehash() {
        Core<V> oldCore = core;
        Core<V> newCore = new Core<>(oldCore.shift - 1);
        for (int i = 0; i < oldCore.length; i++) {
            long key = oldCore.keys[i];
            if (key != NULL_KEY && key != EMPTY_KEY) {
                V value = oldCore.values[i].get();
                if (value != null) {
                    newCore.size++;
                    addInternal(newCore, key, value);
                }
            }
        }
        core = newCore;
    }

}
