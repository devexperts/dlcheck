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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.devexperts.dlcheck.TrasformationUtils.*;
import static org.objectweb.asm.Opcodes.*;

class MethodTransformer extends MethodVisitor {

    MethodTransformer(GeneratorAdapter mv, Configuration configuration,
                             String className, String methodName, String fileName, int access, int classVersion) {
        // COMMON LOGIC
        super(TrasformationUtils.ASM_API, mv);
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.mv = mv;
        // SYNCHRONIZED METHODS LOGIC
        this.classVersion = classVersion;
        this.isSynchronized = (access & ACC_SYNCHRONIZED) != 0;
        this.isStatic = (access & ACC_STATIC) != 0;
        // CUSTOM LOCKS USAGES
        for (String s : configuration.lockPatterns())
            this.lockPredicates.add(createMethodInstructionPredicate(s));
        for (String s : configuration.tryLockPatterns())
            this.tryLockPredicates.add(createMethodInstructionPredicate(s));
        for (String s : configuration.unlockPatterns())
            this.unlockPredicates.add(createMethodInstructionPredicate(s));
        lockVar = mv.newLocal(OBJECT_TYPE);
    }

    // ============
    // COMMON LOGIC
    // ============
    private final GeneratorAdapter mv;
    private final String className;
    private final String methodName;
    private final String fileName;
    private int line;

    private void invokeAfterMonitorEnter() {
        // STACK: <MonitorOwner>
        loadLocationId();
        mv.invokeStatic(DL_CHECK_OPS_TYPE, AFTER_MONITOR_ENTER);
    }

    private void invokeBeforeMonitorExit() {
        // STACK: <MonitorOwner>
        mv.invokeStatic(DL_CHECK_OPS_TYPE, BEFORE_MONITOR_EXIT);
    }

    private void loadLocationId() {
        mv.push(LocationManager.getInstance().getLocationId(className, methodName, fileName, line));
    }

    // ============================================================
    // SYNCHRONIZED METHOD INSTRUMENTATION AND PART OF COMMON LOGIC
    // ============================================================
    private final boolean isSynchronized;
    private final boolean isStatic;
    private final int classVersion;

    private final Label tryLabel = new Label();
    private final Label finallyLabel = new Label();

    private boolean firstInstruction = true;
    private Label label = null;

    @Override
    public void visitLineNumber(int line, Label start) {
        this.line = line;
        super.visitLineNumber(line, start);
        if (firstInstruction && isSynchronized) {
            loadSynchronizedMethodMonitorOwner();
            invokeAfterMonitorEnter();
            mv.visitLabel(tryLabel);
            if (label != null)
                mv.visitLabel(label);
            firstInstruction = false;
            label = null;
        }
    }

    @Override
    public void visitLabel(Label label) {
        if (firstInstruction && isSynchronized) {
            this.label = label;
            return;
        }
        super.visitLabel(label);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (isSynchronized) {
            mv.visitLabel(finallyLabel);
            loadSynchronizedMethodMonitorOwner();
            invokeBeforeMonitorExit();
            mv.visitInsn(Opcodes.ATHROW);
            mv.visitTryCatchBlock(tryLabel, finallyLabel, finallyLabel, null);
        }
        mv.visitMaxs(maxStack, maxLocals);
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

    // =======================================================================
    // SYNCHRONIZED STATEMENT AND PART OF SYNCHRONIZED METHODS INSTRUMENTATION
    // =======================================================================

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            // SYNCHRONIZED STATEMENT INSTRUMENTATION
            case MONITORENTER:
                mv.dup();
                mv.visitInsn(MONITORENTER);
                mv.checkCast(OBJECT_TYPE);
                invokeAfterMonitorEnter();
                break;
            case MONITOREXIT:
                mv.dup();
                invokeBeforeMonitorExit();
                mv.visitInsn(MONITOREXIT);
                break;

            // SYNCHRONIZED METHODS INSTRUMENTATION
            // Match all return codes
            case ARETURN:
            case DRETURN:
            case FRETURN:
            case IRETURN:
            case LRETURN:
            case RETURN:
                if (isSynchronized) {
                    loadSynchronizedMethodMonitorOwner();
                    invokeBeforeMonitorExit();
                }
                mv.visitInsn(opcode);
                break;
            default:
                mv.visitInsn(opcode);
        }
    }

    // ===================================
    // CUSTOM LOCKS USAGES INSTRUMENTATION
    // ===================================
    private final List<MethodInstructionPredicate> lockPredicates = new ArrayList<>();
    private final List<MethodInstructionPredicate> tryLockPredicates = new ArrayList<>();
    private final List<MethodInstructionPredicate> unlockPredicates = new ArrayList<>();
    private final int lockVar;

    private MethodInstructionPredicate createMethodInstructionPredicate(String s) {
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

    public void visitMethodInsn(int opcode, String owner, String mname, String mdesc, boolean itf) {
        boolean isLock = false;
        for (MethodInstructionPredicate p : lockPredicates) {
            if (p.apply(opcode, owner, mname, mdesc, itf)) {
                isLock = true;
                break;
            }
        }
        if (isLock) {
            storeLockInstance(mdesc);
            mv.visitMethodInsn(opcode, owner, mname, mdesc, itf);
            mv.loadLocal(lockVar);
            invokeAfterMonitorEnter();
            return;
        }

        boolean isTryLock = false;
        for (MethodInstructionPredicate p : tryLockPredicates) {
            if (p.apply(opcode, owner, mname, mdesc, itf)) {
                isTryLock = true;
                break;
            }
        }
        if (isTryLock) {
            storeLockInstance(mdesc);
            mv.visitMethodInsn(opcode, owner, mname, mdesc, itf);
            mv.dup();
            Label l = new Label();
            mv.visitJumpInsn(IFEQ, l);
            mv.loadLocal(lockVar);
            invokeAfterMonitorEnter();
            mv.visitLabel(l);
            return;
        }

        boolean isUnlock = false;
        for (MethodInstructionPredicate p : unlockPredicates) {
            if (p.apply(opcode, owner, mname, mdesc, itf)) {
                isUnlock = true;
                break;
            }
        }
        if (isUnlock) {
            storeLockInstance(mdesc);
            mv.loadLocal(lockVar);
            invokeBeforeMonitorExit();
            mv.visitMethodInsn(opcode, owner, mname, mdesc, itf);
            return;
        }
        mv.visitMethodInsn(opcode, owner, mname, mdesc, itf);
    }

    private void storeLockInstance(String mdesc) {
        Type mtype = Type.getMethodType(mdesc);
        Type[] argumentTypes = mtype.getArgumentTypes();
        int n = argumentTypes.length;
        int[] localVars = new int[n];
        for (int i = 0; i < n; i++) {
            localVars[i] = mv.newLocal(argumentTypes[n - i - 1]);
            mv.storeLocal(localVars[i]);
        }
        mv.dup();
        mv.storeLocal(lockVar);
        for (int i = argumentTypes.length - 1; i >= 0; i--) {
            mv.loadLocal(localVars[i]);
        }
    }

    private interface MethodInstructionPredicate {
        boolean apply(int opcode, String owner, String name, String desc, boolean itf);
    }
}
