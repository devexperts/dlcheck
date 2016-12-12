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

import com.devexperts.jagent.ClassInfoVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Modifier;

import static com.devexperts.dlcheck.TrasformationUtils.*;
import static org.objectweb.asm.Opcodes.MONITORENTER;

public class DlCheckClassInfoVisitor extends ClassInfoVisitor {
    private boolean isAbstract;
    private boolean implementsLockNodeHolder;
    private boolean hasSynchronized;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (Modifier.isInterface(access) || Modifier.isAbstract(access))
            isAbstract = false;
        for (String i : interfaces) {
            if (LOCK_NODE_HOLDER_INT_NAME.equals(i))
                implementsLockNodeHolder = true;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Modifier.isSynchronized(access))
            hasSynchronized = true;
        return new MethodVisitor(ASM_API, super.visitMethod(access, name, desc, signature, exceptions)) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == MONITORENTER)
                    hasSynchronized = true;
                super.visitInsn(opcode);
            }
        };
    }

    boolean implementLockNodeHolder() {
        if (isAbstract) // it's all right for abstract classes, but illegal for interfaces
            return false;
        if (implementsLockNodeHolder)
            return false;
        return hasSynchronized;
    }
}