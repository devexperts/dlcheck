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

/**
 * This listener handles Dl-Check events and could be set via
 * {@link DlCheckUtils#addPotentialDeadlockListener(PotentialDeadlockListener)} method.
 */
public interface PotentialDeadlockListener {
    /**
     * Invokes when a new potential deadlock is found
     * @param potentialDeadlock found potential deadlock
     */
    default void onPotentialDeadlock(PotentialDeadlock potentialDeadlock) {};

    /**
     * Invokes when a stacktrace for an edges from an already found cycle is added
     * @param cycleEdge updated cycle edge with stacktrace
     */
    default void onFoundStackTrace(CycleEdge cycleEdge) {};
}

