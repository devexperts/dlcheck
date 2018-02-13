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

import javax.xml.bind.annotation.*;
import java.io.PrintStream;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class PotentialDeadlock {
    private final Cycle cycle;
    private final List<CycleNode> lockStack;

    // Stub for JAXB
    private PotentialDeadlock() {
        cycle = null;
        lockStack = null;
    }

    public PotentialDeadlock(Cycle cycle, List<CycleNode> lockStack) {
        this.cycle = cycle;
        this.lockStack = lockStack;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public List<CycleNode> getLockStack() {
        return lockStack;
    }

    public void print(PrintStream out) {
        PrintUtils.printAsBigHeader(out, "!!! Potential deadlock !!!");
        PrintUtils.printAsSmallHeader(out, "Cycle in lock graph:");
        cycle.print(out);
        PrintUtils.printAsSmallHeader(out, "Current lock stack:");
        for (CycleNode node : lockStack)
            node.print(out);
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
