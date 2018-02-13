package com.devexperts.dlcheck.api.xml;

/*
 * #%L
 * api
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
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


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class StackTraceElementAdapter extends XmlAdapter<StackTraceElementAdapter.SerializableStackTraceElement, StackTraceElement> {
    @Override
    public StackTraceElement unmarshal(SerializableStackTraceElement v) throws Exception {
        return v.toStackTraceElement();
    }

    @Override
    public SerializableStackTraceElement marshal(StackTraceElement v) throws Exception {
        SerializableStackTraceElement e = new SerializableStackTraceElement();
        e.declaredClass = v.getClassName();
        e.method = v.getMethodName();
        e.file = v.getFileName();
        e.line = v.getLineNumber();
        return e;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "element")
    public static class SerializableStackTraceElement {
        @XmlElement(name = "class")
        private String declaredClass;
        private String method;
        private String file;
        private int line;

        private StackTraceElement toStackTraceElement() {
            return new StackTraceElement(declaredClass, method, file, line);
        }
    }
}