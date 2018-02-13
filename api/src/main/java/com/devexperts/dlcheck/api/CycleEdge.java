package com.devexperts.dlcheck.api;

/*
* #%L
 * * core
 * *
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * *
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

// Instance of this class corresponds to a particular edge
// which is lying on the cycle in LockGraph
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class CycleEdge {
    // Unique per JVM-run
    private static AtomicInteger idGenerator = new AtomicInteger();
    // Needed to reference same instances during marshalling
    @XmlAttribute
    public int id;
    @XmlElement(name = "from")
    private CycleNode from;
    @XmlElement(name = "to")
    private CycleNode to;
    // Stack trace of program at the moment when we add edge,
    // not-null if we know that edge is cyclic before adding
    @XmlElementWrapper
    @XmlElement(name = "at")
    @XmlJavaTypeAdapter(StackTraceElementAdapter.class)
    protected StackTraceElement[] stackTrace;

    public CycleNode getFromNode() {
        return from;
    }

    public CycleNode getToNode() {
        return to;
    }

    // JAXB stub
    private CycleEdge() {
    }

    public CycleEdge(CycleNode from, CycleNode to) {
        id = idGenerator.getAndIncrement();
        this.from = from;
        this.to = to;
    }

    public void print(PrintStream out) {
        if (stackTrace != null) {
            out.println("Edge '" + getDesc() + "' was added at:");
            PrintUtils.printStacktrace(out, stackTrace);
        }
    }

    public String getDesc() {
        return from.getDesc() + " -> " + to.getDesc();
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = DlCheckUtils.filterStacktrace(stackTrace);
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CycleEdge cycleEdge = (CycleEdge) o;
        return Objects.equals(from, cycleEdge.from) &&
                Objects.equals(to, cycleEdge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
