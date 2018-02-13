package com.devexperts.dlcheck.api;

/*
 * #%L
 * api
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

import com.devexperts.dlcheck.api.xml.StackTraceElementAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;

@XmlAccessorType(XmlAccessType.NONE)
public class CycleNode {
    private final String desc;
    @XmlElementWrapper
    @XmlElement(name = "at")
    @XmlJavaTypeAdapter(StackTraceElementAdapter.class)
    private final StackTraceElement[] acquiringLocations;

    // JAXB stub
    private CycleNode() {
        acquiringLocations = null;
        desc = null;
    }

    public CycleNode(String desc, Set<StackTraceElement> acquiringLocations) {
        this.desc = desc;
        this.acquiringLocations = acquiringLocations.toArray(new StackTraceElement[0]);
    }

    public StackTraceElement[] getAcquiringLocations() {
        return acquiringLocations;
    }

    public void print(PrintStream out) {
        out.println("Lock " + desc + " was acquired at:");
        PrintUtils.printStacktrace(out, acquiringLocations);
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CycleNode node = (CycleNode) o;

        return Arrays.equals(acquiringLocations, node.acquiringLocations);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(acquiringLocations);
    }
}
