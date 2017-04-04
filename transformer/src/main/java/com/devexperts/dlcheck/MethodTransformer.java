package com.devexperts.dlcheck;

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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.dlcheck.TrasformationUtils.*;

class MethodTransformer extends MethodVisitor {

    MethodTransformer(GeneratorAdapter mv, String className, String methodName, String fileName) {
        super(TrasformationUtils.ASM_API, mv);
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.mv = mv;
    }

    protected final GeneratorAdapter mv;
    protected final String className;
    protected final String methodName;
    protected final String fileName;
    protected int line;

    protected void invokeAfterMonitorEnter() {
        // STACK: <MonitorOwner>
        loadLocationId();
        mv.invokeStatic(DL_CHECK_OPS_TYPE, AFTER_MONITOR_ENTER);
    }

    protected void invokeAfterMonitorExit() {
        // STACK: <MonitorOwner>
        mv.invokeStatic(DL_CHECK_OPS_TYPE, AFTER_MONITOR_EXIT);
    }

    private void loadLocationId() {
        mv.push(LocationManager.getInstance().getLocationId(className, methodName, fileName, line));
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.line = line;
        super.visitLineNumber(line, start);
    }
}