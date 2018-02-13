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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import java.util.List;

import static com.devexperts.dlcheck.TrasformationUtils.*;
import static org.objectweb.asm.Opcodes.*;

public class ClassTransformer extends ClassVisitor {
    private final String fileName;
    private final List<MethodInstructionPredicate> lockPredicates;
    private final List<MethodInstructionPredicate> tryLockPredicates;
    private final List<MethodInstructionPredicate> unlockPredicates;
    private final boolean implementLockNodeHolder;
    private String className;
    private int classVersion;

    ClassTransformer(ClassVisitor cv, String fileName, List<MethodInstructionPredicate> lockPredicates,
        List<MethodInstructionPredicate> tryLockPredicates, List<MethodInstructionPredicate> unlockPredicates,
        boolean implementLockNodeHolder)
    {
        super(ASM_API, cv);
        this.fileName = fileName;
        this.lockPredicates = lockPredicates;
        this.tryLockPredicates = tryLockPredicates;
        this.unlockPredicates = unlockPredicates;
        this.implementLockNodeHolder = implementLockNodeHolder;
    }

    @Override
    public void visitEnd() {
        if (implementLockNodeHolder) {
            FieldVisitor fv;
            MethodVisitor mv;

            fv = cv.visitField(ACC_TRANSIENT + ACC_PRIVATE, LOCK_NODE_FIELD_NAME, LOCK_NODE_DESC, null, null);
            fv.visitEnd();

            mv= cv.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, GET_LOCK_NODE_METHOD_NAME, "()" + LOCK_NODE_DESC, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, LOCK_NODE_FIELD_NAME, LOCK_NODE_DESC);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            mv = cv.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, SET_LOCK_NODE_METHOD_NAME, "(" + LOCK_NODE_DESC + ")V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, LOCK_NODE_FIELD_NAME, LOCK_NODE_DESC);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        super.visitEnd();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        classVersion = version;
        if (implementLockNodeHolder) {
            String[] is = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, is, 0, interfaces.length);
            is[is.length - 1] = LOCK_NODE_HOLDER_INT_NAME;
            interfaces = is;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        mv = new JSRInlinerAdapter(mv, access, methodName, desc, signature, exceptions);
        mv = new SynchronizedBlockTransformer(new GeneratorAdapter(mv, access, methodName, desc),
            className, methodName, fileName);
        mv = new SynchronizedMethodTransformer(new GeneratorAdapter(mv, access, methodName, desc),
            className, methodName, fileName, access, classVersion);
        mv = new LockTransformer(new GeneratorAdapter(mv, access, methodName, desc),
            lockPredicates, tryLockPredicates, unlockPredicates, className, methodName, fileName);
        mv = new TryCatchBlockSorter(mv, access, methodName, desc, signature, exceptions);
        return mv;
    }
}
