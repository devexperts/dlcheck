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


import com.devexperts.dlcheck.api.CycleEdge;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;

public class CycleEdgeAdapter extends XmlAdapter<CycleEdgeAdapter.CycleEdgeReference, CycleEdge> {
    private static HashMap<Integer, CycleEdge> unmarshalledEdges;

    @Override
    public CycleEdge unmarshal(CycleEdgeReference edge) throws Exception {
        return edge.toCycleEdge();
    }

    @Override
    public CycleEdgeReference marshal(CycleEdge edge) throws Exception {
        return new CycleEdgeReference(edge);
    }

    // Must be called when unmarshalling CycleEdge
    public static void registerUnmarshalled(CycleEdge edge) {
        if(unmarshalledEdges == null) {
            unmarshalledEdges = new HashMap<>();
        }
        unmarshalledEdges.put(edge.id, edge);
    }

    static class CycleEdgeReference {
        @XmlAttribute
        final int id;

        private CycleEdgeReference() {
            id = 0;
        }

        CycleEdgeReference(CycleEdge edge) {
            id = edge.id;
        }

        CycleEdge toCycleEdge() {
            return unmarshalledEdges.get(id);
        }
    }
}
