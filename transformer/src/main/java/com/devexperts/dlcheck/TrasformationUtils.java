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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.Externalizable;
import java.io.Serializable;

import static org.objectweb.asm.Opcodes.ASM5;

class TrasformationUtils {
    static final int ASM_API = ASM5;

    static final Type OBJECT_TYPE = Type.getType(Object.class);
    static final Type STRING_TYPE = Type.getType(String.class);
    static final Type DL_CHECK_OPS_TYPE = Type.getType(DlCheckOperations.class);
    static final Type CLASS_TYPE = Type.getType(Class.class);
    static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

    static final Method AFTER_MONITOR_ENTER = new Method("afterMonitorEnter", Type.VOID_TYPE, new Type[]{OBJECT_TYPE, Type.INT_TYPE});
    static final Method AFTER_MONITOR_EXIT = new Method("afterMonitorExit", Type.VOID_TYPE, new Type[]{OBJECT_TYPE});
    static final Method CLASS_FOR_NAME = new Method("forName", CLASS_TYPE, new Type[]{STRING_TYPE});
    static final Method EXCEPTION_PRINT_STACKTRACE = new Method("printStackTrace", Type.VOID_TYPE, new Type[]{});

    static final String  LOCK_NODE_DESC = Type.getType(LockNode.class).getDescriptor();
    static final String GET_LOCK_NODE_METHOD_NAME = "__dlcheck_get_lock_node__";
    static final String SET_LOCK_NODE_METHOD_NAME = "__dlcheck_set_lock_node__";
    static final String LOCK_NODE_FIELD_NAME = "__com_devexperts_dlcheck_lock_node__";
    static final String LOCK_NODE_HOLDER_INT_NAME = Type.getType(LockNodeHolder.class).getInternalName();

    static final String SERIALIZABLE_INT_NAME = Type.getType(Serializable.class).getInternalName();
    static final String EXTERNALIZABLE_INT_NAME = Type.getType(Externalizable.class).getInternalName();
    static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";

    /**
     * Wrap the generated code in safe try-catch block.
     *
     * It creates an equivalent bytecode:
     * <pre>
     * <code>
     * try {
     *     // Generated code will be here
     * } catch (Throwable t) {
     *     // Ignore
     * }
     * </code>
     * </pre>
     */
    public static void wrapCodeToMakeItSafe(GeneratorAdapter mv, Runnable generatedCode) {
        if (true) {
            // TODO fix it
            generatedCode.run();
            return;
        }
       // Create labels and configure try-catch blocks
       Label tryLabel = mv.newLabel();
       Label catchLabel = mv.newLabel();
       Label endLabel = mv.newLabel();
       mv.visitTryCatchBlock(tryLabel, catchLabel, catchLabel, null);
       // Start try block
       mv.mark(tryLabel);
           // === GENERATED CODE ===
           generatedCode.run();
           // === GENERATED CODE ===
           // If code is executed successfully, go to the end
           mv.goTo(endLabel);
        // Start catch block
        mv.mark(catchLabel);
            // Just ignore this exception
            mv.pop();
        mv.mark(endLabel);
    }
}
