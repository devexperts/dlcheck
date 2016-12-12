package com.devexperts.dlcheck.tests.base;

/*
 * #%L
 * test
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

import com.devexperts.dlcheck.api.DlCheckUtils;
import com.devexperts.dlcheck.api.PotentialDeadlockListener;
import org.junit.After;
import org.junit.Before;

public class LockTestBase {

    protected int potentialDeadlocksCount;
    private final PotentialDeadlockListener PDL = potentialDeadlock -> {
        potentialDeadlocksCount++;
    };

    @Before
    public void setUp() {
        DlCheckUtils.addPotentialDeadlockListener(PDL);
    }

    @After
    public void tearDown() {
        DlCheckUtils.removePotentialDeadlockListener(PDL);
    }
}
