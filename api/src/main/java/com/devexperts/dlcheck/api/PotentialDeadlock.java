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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class PotentialDeadlock {
    private final Cycle cycle;
    private final List<StackTraceElement> acquireStackTrace;
    private final List<CycleNode> lockStack;

    private PotentialDeadlock(Cycle cycle, List<StackTraceElement> acquireStackTrace, List<CycleNode> lockStack) {
        this.cycle = cycle;
        this.acquireStackTrace = acquireStackTrace;
        this.lockStack = lockStack;
    }

    public static PotentialDeadlock create(Cycle cycle, List<CycleNode> lockStack) {
        List<StackTraceElement> acquireStackTrace = Arrays.asList(filterStacktrace(new Exception().getStackTrace()));
        return new PotentialDeadlock(cycle, acquireStackTrace, lockStack);
    }

    /**
     * Removes dl-check's part of stack trace.
     */
    private static StackTraceElement[] filterStacktrace(StackTraceElement[] stackTrace) {
        int lastIndexWithDlCheck = -1;
        for (int i = 0; i < stackTrace.length; i++) {
            if (stackTrace[i].getClassName().startsWith("com.devexperts.dlcheck.") && !stackTrace[i].getClassName().startsWith("com.devexperts.dlcheck.tests."))
                lastIndexWithDlCheck = i;
        }
        return Arrays.copyOfRange(stackTrace, lastIndexWithDlCheck + 1, stackTrace.length);
    }

    public Cycle getCycle() {
        return cycle;
    }

    public List<CycleNode> getLockStack() {
        return lockStack;
    }

    public List<StackTraceElement> getAcquireStackTrace() {
        return acquireStackTrace;
    }

    public void print(PrintStream out) {
        PrintUtils.printAsBigHeader(out, "!!! Potential deadlock !!!");
        PrintUtils.printAsSmallHeader(out, "Cycle in lock graph:");
        cycle.print(out);
        PrintUtils.printAsSmallHeader(out, "Current lock stack:");
        for (CycleNode node : lockStack)
            node.print(out);
        PrintUtils.printAsSmallHeader(out, "Current stacktrace:");
        PrintUtils.printStacktrace(out, acquireStackTrace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PotentialDeadlock that = (PotentialDeadlock) o;

        if (cycle != null ? !cycle.equals(that.cycle) : that.cycle != null) return false;
        return lockStack != null ? lockStack.equals(that.lockStack) : that.lockStack == null;

    }

    @Override
    public int hashCode() {
        int result = cycle != null ? cycle.hashCode() : 0;
        result = 31 * result + (lockStack != null ? lockStack.hashCode() : 0);
        return result;
    }
}
