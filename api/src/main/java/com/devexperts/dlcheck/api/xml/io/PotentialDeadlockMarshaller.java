package com.devexperts.dlcheck.api.xml.io;

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
import com.devexperts.dlcheck.api.FileUtils;
import com.devexperts.dlcheck.api.PotentialDeadlock;
import com.devexperts.dlcheck.api.xml.NewEdgeStackTrace;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

// Contains methods to serialize data about found potential deadlocks
public class PotentialDeadlockMarshaller {
    public static final String XML_OUT_PROPERTY = "dlcheck.xml.file";
    private static final boolean ENABLED;
    private static volatile PrintStream out;
    private static volatile Marshaller pdMarshaller;
    private static volatile Marshaller edgeMarshaller;
    private static volatile Marshaller newStackTraceMarshaller;
    // Contains ids of marshalled CycleEdge instances
    private static final Set<Integer> marshalledEdges = new HashSet<>();

    private PotentialDeadlockMarshaller() {
    }

    static {
        boolean isEnabled = false;
        try {
            String file = System.getProperty(XML_OUT_PROPERTY);
            if (file != null) {
                Path filePath = Paths.get(file);
                OutputStream outputStream;
                FileUtils.createMissingDirectories(filePath);
                outputStream = Files.newOutputStream(filePath);
                outputStream.flush();
                out = new PrintStream(outputStream);
                out.println("<dlcheck>");
                edgeMarshaller = getMarshaller(CycleEdge.class);
                pdMarshaller = getMarshaller(PotentialDeadlock.class);
                newStackTraceMarshaller = getMarshaller(NewEdgeStackTrace.class);
                isEnabled = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    out.println("</dlcheck>");
                    out.close();
                }));
            }
        } catch (JAXBException | IOException ex) {
            ex.printStackTrace();
        }
        ENABLED = isEnabled;
    }

    private static <T> Marshaller getMarshaller(Class<T> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        return marshaller;
    }

    public static synchronized void marshallPotentialDeadlock(PotentialDeadlock potentialDeadlock) {
        // First marshall edges and then reference to them
        for (CycleEdge edge : potentialDeadlock.getCycle().getEdges()) {
            if (marshalledEdges.add(edge.id)) {
                marshall(edgeMarshaller, edge);
            }
        }
        marshall(pdMarshaller, potentialDeadlock);
    }

    public static synchronized void marshallEdgeStackTrace(CycleEdge edge) {
        marshall(newStackTraceMarshaller, new NewEdgeStackTrace(edge, edge.getStackTrace()));
    }

    private static void marshall(Marshaller marshaller, Object obj) {
        if (ENABLED) {
            try {
                marshaller.marshal(obj, out);
            } catch (JAXBException ex) {
                ex.printStackTrace();
            }
            out.flush();
        }
    }
}
