package com.devexperts.dlcheck.api.xml.io;

/*
* #%L
 * * api
 * *
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
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


import com.devexperts.dlcheck.api.CycleEdge;
import com.devexperts.dlcheck.api.PotentialDeadlock;
import com.devexperts.dlcheck.api.xml.CycleEdgeAdapter;
import com.devexperts.dlcheck.api.xml.NewEdgeStackTrace;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.HashMap;

public class WatchTask {

    // Used to handle just unmarshalled objects
    public interface Handler<T> {
        void handle(T obj);
    }

    // What we have started to read but haven't finished
    private enum State {
        DEADLOCK, STACKTRACE, EDGE, NONE
    }

    private BufferedReader br;
    private StringBuilder sb = new StringBuilder();
    private State state = State.NONE;
    private boolean finish = false;
    private final Unmarshaller edgeUnmarshaller;
    private final Unmarshaller deadlockUnmarshaller;
    private final Unmarshaller stackTraceUnmarshaller;
    private static final HashMap<State, String> closingTag = new HashMap<>();
    private static final String POTENTIAL_DEADLOCK_TAG_NAME = "potentialDeadlock";
    private static final String EDGE_TAG_NAME = "cycleEdge";
    private static final String NEW_EDGE_STACK_TRACE_TAG_NAME = "newEdgeStackTrace";
    private static final String DLCHECK_TAG_NAME = "dlcheck";

    static {
        closingTag.put(State.EDGE, "</" + EDGE_TAG_NAME + ">");
        closingTag.put(State.DEADLOCK, "</" + POTENTIAL_DEADLOCK_TAG_NAME + ">");
        closingTag.put(State.STACKTRACE, "</" + NEW_EDGE_STACK_TRACE_TAG_NAME + ">");
    }

    private Handler<CycleEdge> newStackTraceHandler;
    private Handler<PotentialDeadlock> potentialDeadlockHandler;

    public void setNewStackTraceHandler(Handler<CycleEdge> newStackTraceHandler) {
        this.newStackTraceHandler = newStackTraceHandler;
    }

    public void setPotentialDeadlockHandler(Handler<PotentialDeadlock> potentialDeadlockHandler) {
        this.potentialDeadlockHandler = potentialDeadlockHandler;
    }

    public WatchTask(File file) throws IOException {
        br = new BufferedReader(new FileReader(file));
        edgeUnmarshaller = getUnmarshaller(CycleEdge.class);
        deadlockUnmarshaller = getUnmarshaller(PotentialDeadlock.class);
        stackTraceUnmarshaller = getUnmarshaller(NewEdgeStackTrace.class);
    }

    public boolean isFinish() {
        return finish;
    }

    private <T> Unmarshaller getUnmarshaller(Class<T> clazz) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            return context.createUnmarshaller();
        } catch (JAXBException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void tryRead() throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isEmpty() || line.equalsIgnoreCase("<" + DLCHECK_TAG_NAME + ">")) {
                continue;
            }
            if (line.equalsIgnoreCase("</" + DLCHECK_TAG_NAME + ">")) {
                finish = true;
                break;
            }
            if (state == State.NONE) {
                if (line.startsWith("<" + EDGE_TAG_NAME)) {
                    state = State.EDGE;
                } else if (line.startsWith("<" + POTENTIAL_DEADLOCK_TAG_NAME)) {
                    state = State.DEADLOCK;
                } else if (line.startsWith("<" + NEW_EDGE_STACK_TRACE_TAG_NAME)) {
                    state = State.STACKTRACE;
                }
            }
            sb.append(line);
            if (line.equalsIgnoreCase(closingTag.get(state))) {
                try {
                    StringReader reader = new StringReader(sb.toString());
                    sb = new StringBuilder();
                    switch (state) {
                        case DEADLOCK:
                            PotentialDeadlock pd = (PotentialDeadlock) deadlockUnmarshaller.unmarshal(reader);
                            if (potentialDeadlockHandler != null) {
                                potentialDeadlockHandler.handle(pd);
                            }
                            break;
                        case STACKTRACE:
                            NewEdgeStackTrace res = (NewEdgeStackTrace) stackTraceUnmarshaller.unmarshal(reader);
                            CycleEdge edge = res.getEdge();
                            // Need to attach recently found stacktrace to the corresponding edge
                            edge.setStackTrace(res.getStackTrace());
                            if (newStackTraceHandler != null) {
                                newStackTraceHandler.handle(edge);
                            }
                            break;
                        case EDGE:
                            CycleEdge cycleEdge = (CycleEdge) edgeUnmarshaller.unmarshal(reader);
                            // We will reference this edge in the future
                            CycleEdgeAdapter.registerUnmarshalled(cycleEdge);
                            break;
                    }
                    state = State.NONE;
                } catch (JAXBException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
