package com.devexperts.dlcheck.api;

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

import com.devexperts.dlcheck.api.xml.CycleEdgeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@XmlAccessorType(XmlAccessType.NONE)
public class Cycle {
    @XmlElementWrapper(name = "edges")
    @XmlElement(name = "edge")
    @XmlJavaTypeAdapter(CycleEdgeAdapter.class)
    private final List<CycleEdge> edges;

    // JAXB stub
    private Cycle() {
        edges = null;
    }

    public Cycle(List<CycleEdge> edges) {
        this.edges = edges;
    }

    public void print(PrintStream out) {
        for (CycleEdge edge : edges) {
            edge.getFromNode().print(out);
            edge.print(out);
        }
    }

    public List<CycleEdge> getEdges() {
        return edges;
    }

    /**
     * @deprecated use {@link #getEdges()} instead.
     */
    public List<CycleNode> getNodes() {
        return edges.stream().map(CycleEdge::getFromNode).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cycle cycle = (Cycle) o;

        return edges != null ? edges.equals(cycle.edges) : cycle.edges == null;
    }

    @Override
    public int hashCode() {
        return edges != null ? edges.hashCode() : 0;
    }
}
