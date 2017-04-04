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

import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.dlcheck.TrasformationUtils.OBJECT_TYPE;
import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;

public class SynchronizedBlockTransformer extends MethodTransformer {

    SynchronizedBlockTransformer(GeneratorAdapter mv,
        String className, String methodName, String fileName)
    {
        super(mv, className, methodName, fileName);
    }

    @Override
    public void visitInsn(int opcode) {
        int monitorLocal;
        switch (opcode) {
        case MONITORENTER:
            monitorLocal = mv.newLocal(OBJECT_TYPE);
            // Store monitor to local variable
            mv.dup();
            mv.storeLocal(monitorLocal);
            // == MONITORENTER ==
            mv.monitorEnter();
            // Create labels
            Label tryLabel = mv.newLabel();
            Label catchLabel = mv.newLabel();
            Label endLabel = mv.newLabel();
            mv.visitTryCatchBlock(tryLabel, catchLabel, catchLabel, null);
            // Start try block and invoke afterMonitorEnter
            mv.mark(tryLabel);
            mv.loadLocal(monitorLocal);
            invokeAfterMonitorEnter();
            mv.goTo(endLabel);
            // Start catch block
            mv.mark(catchLabel);
            mv.loadLocal(monitorLocal);
            mv.monitorExit();
            mv.throwException();
            // Finish catch block
            mv.mark(endLabel);
            break;
        case MONITOREXIT:
            monitorLocal = mv.newLocal(OBJECT_TYPE);
            // Store monitor to local variable
            mv.dup();
            mv.storeLocal(monitorLocal);
            // == MONITOREXIT ==
            mv.monitorExit();
            // Invoke afterMonitorExit
            TrasformationUtils.wrapCodeToMakeItSafe(mv, () -> {
                mv.loadLocal(monitorLocal);
                invokeAfterMonitorExit();
            });
            break;
        default:
            mv.visitInsn(opcode);
        }
    }
}
