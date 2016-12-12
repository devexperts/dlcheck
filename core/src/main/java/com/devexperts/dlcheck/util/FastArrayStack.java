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

public class FastArrayStack<T> {
    private static final int MINIMUM_SIZE = 8;

    private Object[] stack = new Object[MINIMUM_SIZE];
    private int size;

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    public void push(T t) {
        if (size == stack.length) {
            // Grow up
            T[] a = (T[]) new Object[2 * size];
            System.arraycopy(stack, 0, a, 0, size);
            stack = a;
        }
        stack[size++] = t;
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    public T pop() {
        return (T) stack[--size];
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
        Arrays.fill(stack, null);
    }
}