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

public class FastLongArrayList {
    private long[] a;
    private int size;

    public FastLongArrayList() {
        a = new long[128];
    }

    public long get(int i) {
        if (i >= size || i < 0)
            throw new ArrayIndexOutOfBoundsException();
        return a[i];
    }

    public void put(int i, long val) {
        if (i >= size || i < 0)
            throw new ArrayIndexOutOfBoundsException();
        a[i] = val;
    }

    public void add(long val) {
        if (size == a.length) {
            long[] newA = new long[2 * a.length];
            System.arraycopy(a, 0, newA, 0, size);
            a = newA;
        }
        a[size++] = val;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public void sort() {
        Arrays.sort(a);
    }
}
