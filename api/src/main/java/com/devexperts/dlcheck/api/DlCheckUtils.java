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

import java.util.LinkedHashSet;
import java.util.Set;

public class DlCheckUtils {
    private static final Set<PotentialDeadlockListener> POTENTIAL_DEADLOCK_LISTENERS = new LinkedHashSet<>();

    private static final boolean FAIL_ON_POTENTIAL_DEADLOCK = Boolean.parseBoolean(System.getProperty("dlcheck.fail", "false"));

    static {
        addPotentialDeadlockListener(PotentialDeadlockPublisher::publishPotentialDeadlock);
    }

    public static synchronized void addPotentialDeadlockListener(PotentialDeadlockListener listener) {
        POTENTIAL_DEADLOCK_LISTENERS.add(listener);
    }

    public static synchronized void removePotentialDeadlockListener(PotentialDeadlockListener listener) {
        POTENTIAL_DEADLOCK_LISTENERS.remove(listener);
    }

    public static synchronized void notifyAboutPotentialDeadlock(PotentialDeadlock potentialDeadlock) {
        for (PotentialDeadlockListener listener : POTENTIAL_DEADLOCK_LISTENERS)
            listener.handle(potentialDeadlock);
        if (FAIL_ON_POTENTIAL_DEADLOCK)
            throw new AssertionError("Potential deadlock have detected, see Dl-Check logs for details");
    }
}