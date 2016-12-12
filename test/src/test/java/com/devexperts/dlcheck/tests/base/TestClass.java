package com.devexperts.dlcheck.tests.base;

/*
 * #%L
 * transformer
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

import com.devexperts.dlcheck.LockNode;
import com.devexperts.dlcheck.LockNodeHolder;
import org.objectweb.asm.util.ASMifier;

import java.io.Serializable;

class TestClass {

    public static void main(String[] args) throws Exception {
        ASMifier.main(new String[]{"com.devexperts.dlcheck.tests.base.Test$B"});
    }

    class A implements LockNodeHolder {

        private LockNode x;

        @Override
        public LockNode __dlcheck_get_lock_node__() {
            return x;
        }

        @Override
        public void __dlcheck_set_lock_node__(LockNode node) {
            x = node;
        }
    }

    class  B implements Serializable {}

}
