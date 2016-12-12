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

import com.devexperts.dlcheck.LockNode;

import java.util.Iterator;

public class LockNodeStack implements Iterable<LockNode> {
    private LockNode[] stack;
    private int size;

    public LockNodeStack() {
        stack = new LockNode[10];
    }

    public void push(LockNode node) {
        if (node == null)
            throw new IllegalArgumentException("Cannot push null");
        if (size >= stack.length) {
            LockNode[] a = new LockNode[2 * stack.length];
            System.arraycopy(stack, 0, a, 0, stack.length);
            stack = a;
        }
        stack[size++] = node;
    }

    // returns true if element was removed from top
    public boolean remove(LockNode node) {
        // Just decrement size if element is on top
        if (size > 0 && stack[size - 1] == node) {
            stack[--size] = null;
            return true;
        }
        // Cannot remove from empty lock stack
        if (size == 0)
            return false;
        // In other case shift all elements
        // Firstly, find index of element to be removed
        int index = size - 1;
        while (index >= 0 && stack[index] != node) {
            index--;
        }
        if (index < 0) // Node isn't on stack
            return false;
        // Then shift elements at right side
        while (index < size - 1) {
            stack[index] = stack[index + 1];
            index++;
        }
        stack[size - 1] = null;
        size--;
        return false;
    }

    public LockNode[] cloneAsArray() {
        LockNode[] res = new LockNode[size()];
        System.arraycopy(stack, 0, res, 0, size());
        return res;
    }

    public LockNode get(int i) {
        return stack[i];
    }

    public int size() {
        return size;
    }

    @Override
    public Iterator<LockNode> iterator() {
        return new Iterator<LockNode>() {
            int i = size - 1;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @SuppressWarnings("unchecked")
            @Override
            public LockNode next() {
                return stack[i--];
            }
        };
    }
}