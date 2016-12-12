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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LockNodeFieldInjecterTest {

    public int x;

    @Test
    @Ignore
    public void testSerializable() {
        SerializableClass serializable = new SerializableClass();
        synchronized (serializable) {
            x++;
        }
        Assert.assertEquals(3, serializable.getClass().getDeclaredFields().length);
    }

    @Test
    @Ignore
    public void testComplexSerializable() {
        ComplexSerializableClass serializable = new ComplexSerializableClass();
        synchronized (serializable) {
            x++;
        }
        Assert.assertEquals(1, serializable.getClass().getDeclaredFields().length);
    }

    @Test
    @Ignore
    public void testSimpleClassWithSynchronized() {
        SimpleClassWithSynchronized o = new SimpleClassWithSynchronized();
        synchronized (o) {}
        Assert.assertEquals(2, SimpleClassWithSynchronized.class.getDeclaredFields().length);
    }

    @Test
    public void testSimpleClass() {
        SimpleClass o = new SimpleClass();
        synchronized (o) {}
        Assert.assertEquals(1, SimpleClass.class.getDeclaredFields().length);
    }

    private static class SerializableClass implements Serializable {
        int x = 100;
        String s = "AAA";
        List<Integer> list = new ArrayList<>();

        synchronized void f() {}
    }

    private static class ComplexSerializableClass extends SerializableClass {
        Object field;

        synchronized void f() {}
    }

    private static class SimpleClass {
        Object field;
    }

    private static class SimpleClassWithSynchronized {
        Object field;

        synchronized void f() {}
    }
}
