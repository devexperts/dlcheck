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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;

import static org.objectweb.asm.Opcodes.*;

public class Test implements Runnable {

    public static void main(String[] args) throws Exception {
//        Runnable r = new MyClassLoader().load().newInstance();
//        r.run();
        ASMifier.main(new String[]{Test.class.getCanonicalName()});
    }

    @Override
    public void run() {
        System.out.println(0);
    }

    static class MyClassLoader extends ClassLoader {
        public Class<Runnable> load() {
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            CheckClassAdapter cw = new CheckClassAdapter(cw0);
            GeneratorAdapter mv;

            cw.visit(52, ACC_PUBLIC + ACC_SUPER, "Test", null, "java/lang/Object", new String[] { "java/lang/Runnable" });

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null), ACC_PUBLIC, "<init>", "()V");
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null), ACC_PUBLIC, "run", "()V");
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            mv.visitTryCatchBlock(l0, l1, l1, null);

            mv.push(0 );
            // try
            mv.visitLabel(l0);
            mv.push(1);
            mv.push(2);
            mv.pop();
            mv.pop();
            // finally
            mv.visitLabel(l1);
            // end finally
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.swap();
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);

            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();

            cw.visitEnd();
            byte[] bytes = cw0.toByteArray();

            return (Class<Runnable>) defineClass("Test", bytes, 0, bytes.length);
        }
    }
}
