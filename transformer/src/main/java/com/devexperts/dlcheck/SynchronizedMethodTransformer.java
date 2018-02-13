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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.dlcheck.TrasformationUtils.CLASS_FOR_NAME;
import static com.devexperts.dlcheck.TrasformationUtils.CLASS_TYPE;
import static com.devexperts.dlcheck.TrasformationUtils.THROWABLE_TYPE;
import static org.objectweb.asm.Opcodes.*;

class SynchronizedMethodTransformer extends MethodTransformer {
    private final String className;
    private final boolean isSynchronized;
    private final boolean isStatic;
    private final int classVersion;

    private final Label tryLabel = new Label();
    private final Label catchLabel = new Label();

    SynchronizedMethodTransformer(GeneratorAdapter mv,
        String className, String methodName, String fileName, int access, int classVersion)
    {
        super(mv, className, methodName, fileName);
        this.className = className;
        this.classVersion = classVersion;
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        if (isSynchronized) {
            mv.visitTryCatchBlock(tryLabel, catchLabel, catchLabel, null);
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (isSynchronized) {
            TrasformationUtils.wrapCodeToMakeItSafe(mv, () -> {
                loadSynchronizedMethodMonitorOwner();
                invokeAfterMonitorEnter();
            });
            mv.visitLabel(tryLabel);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isSynchronized) {
            mv.visitLabel(catchLabel);
            int throwableLocal = mv.newLocal(THROWABLE_TYPE);
            mv.storeLocal(throwableLocal);
            TrasformationUtils.wrapCodeToMakeItSafe(mv, () -> {
                loadSynchronizedMethodMonitorOwner();
                invokeAfterMonitorExit();
            });
            mv.loadLocal(throwableLocal);
            mv.throwException();
        }
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case ARETURN:
        case DRETURN:
        case FRETURN:
        case IRETURN:
        case LRETURN:
        case RETURN:
            if (isSynchronized) {
                TrasformationUtils.wrapCodeToMakeItSafe(mv, () -> {
                    loadSynchronizedMethodMonitorOwner();
                    invokeAfterMonitorExit();
                });
            }
            mv.visitInsn(opcode);
            break;
        default:
            mv.visitInsn(opcode);
        }
    }

    private void loadSynchronizedMethodMonitorOwner() {
        if (isStatic) {
            Type classType = Type.getType("L" + className + ";");
            if (classVersion >= V1_5) {
                mv.visitLdcInsn(classType);
            } else {
                mv.visitLdcInsn(classType.getClassName());
                mv.invokeStatic(CLASS_TYPE, CLASS_FOR_NAME);
            }
        } else {
            mv.loadThis();
        }
    }

}
