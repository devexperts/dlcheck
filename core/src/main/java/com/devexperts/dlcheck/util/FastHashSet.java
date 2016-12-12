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

import java.util.Arrays;

public class FastHashSet<T> {
    private static final int MAGIC = 0xB46394CD;
    private static final int MAX_SHIFT = 29;
    private static final int THRESHOLD = (int)((1L << 32) * 0.5); // 50% fill factor for speed

    private static class Core {
        final int shift;
        final int length;
        final Object[] keys;

        Core(int shift) {
            this.shift = shift;
            length = 1 << (32 - shift);
            keys = new Object[length];
        }
    }

    private volatile Core core = new Core(MAX_SHIFT);
    private volatile int size;

    // needs external synchronization
    public boolean contains(T key) {
        Core core = this.core;
        int i = (key.hashCode() * MAGIC) >>> core.shift;
        while (true) {
            Object curKey = core.keys[i];
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

    public void clear() {
        Arrays.fill(core.keys, null);
        size = 0;
    }

    private boolean addInternal(Core core, Object key) {
        int i = (System.identityHashCode(key) * MAGIC) >>> core.shift;
        while (true) {
            Object curKey = core.keys[i];
            if (curKey == null) {
                core.keys[i] = key;
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
            Object key = oldCore.keys[i];
            if (key != null)
                addInternal(newCore, key);
        }
        core = newCore;
    }
}
