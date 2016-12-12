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
import java.util.List;

public class Cycle {

    private final List<CycleNode> cycle;

    public Cycle(List<CycleNode> cycle) {
        this.cycle = cycle;
    }

    public void print(PrintStream out) {
        for (CycleNode cn : cycle)
            cn.print(out);
    }

    public List<CycleNode> getNodes() {
        return cycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cycle cycle1 = (Cycle) o;

        return cycle != null ? cycle.equals(cycle1.cycle) : cycle1.cycle == null;

    }

    @Override
    public int hashCode() {
        return cycle != null ? cycle.hashCode() : 0;
    }
}
