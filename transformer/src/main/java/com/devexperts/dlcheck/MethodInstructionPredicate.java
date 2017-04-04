package com.devexperts.dlcheck;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
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

import java.util.regex.Pattern;

interface MethodInstructionPredicate {

    static MethodInstructionPredicate create(String s) {
        s = s.replace('.', '/');
        // Parse string
        int i = s.indexOf("#");
        String className = s.substring(0, i);
        String methodName = s.substring(i + 1);
        // Create patterns
        Pattern classNamePattern = GlobUtil.compile(className);
        Pattern methodNamePattern = GlobUtil.compile(methodName);
        return (opcode, owner, mname, mdesc, itf) -> classNamePattern.matcher(owner).matches() &&
            methodNamePattern.matcher(mname).matches();
    }

    boolean apply(int opcode, String owner, String name, String desc, boolean itf);
}
