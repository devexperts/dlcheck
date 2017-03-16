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

import com.devexperts.jagent.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class Transformer extends CachingClassFileTransformer {
    private final Configuration configuration;
    private final ClassInfoCache ciCache;
    private final List<Pattern> includes;
    private final List<Pattern> excludes;

    Transformer(Configuration configuration, Log log, String agentVersion) {
        super(log, agentVersion);
        this.configuration = configuration;
        this.ciCache = new ClassInfoCache(log);
        includes = new ArrayList<>(configuration.include().length);
        for (String s : configuration.include())
            includes.add(GlobUtil.compile(s.replaceAll("\\.", "/")));
        excludes = new ArrayList<>(configuration.exclude().length);
        for (String s : configuration.exclude())
            excludes.add(GlobUtil.compile(s.replaceAll("\\.", "/")));
    }

    @Override
    protected boolean processClass(String className, ClassLoader classLoader) {
        if ((className.startsWith("com/devexperts/dlcheck")
                && !className.startsWith("com/devexperts/dlcheck/tests/")
                && !className.startsWith("com/devexperts/dlcheck/benchmarks/")) ||
                // Do not analyze java classes
                className.startsWith("sun/") ||
                className.startsWith("java/") ||
                className.startsWith("javax/") ||
                className.startsWith("com/sun/") ||
                className.startsWith("org/openjdk/") ||
                // Do not analyze dependencies
                className.startsWith("org/objectweb/asm/") ||
                className.startsWith("org/aeonbits/owner/") ||
                className.startsWith("com/devexperts/jagent/") ||
                // Do not transform maven
                className.startsWith("org/apache/maven/"))
            return false;
        boolean process = false;
        for (Pattern p : includes) {
            if (p.matcher(className).matches()) {
                process = true;
                break;
            }
        }
        for (Pattern p : excludes) {
            if (p.matcher(className).matches()) {
                process = false;
                break;
            }
        }
        return process;
    }

    @Override
    public byte[] transformImpl(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
        long startTime = System.currentTimeMillis();
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            DlCheckClassInfoVisitor ciVisitor = new DlCheckClassInfoVisitor();
            cr.accept(ciVisitor, 0);
            ClassInfo cInfo = ciVisitor.buildClassInfo();
            ciCache.getOrInitClassInfoMap(loader).put(className, cInfo);
            ClassWriter cw = new FrameClassWriter(loader, ciCache, cInfo.getVersion());
            ClassVisitor classTransformer = new ClassTransformer(
                    ciVisitor.implementLockNodeHolder() ? new SerialVersionUIDAdder(cw) : cw,
                    cInfo.getSourceFile(), configuration, false
            );
            cr.accept(classTransformer, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            log.warn("Unable to transform class ", className, e);
            return null;
        } finally {
            Stats.INSTANCE.increase_transformation_time(System.currentTimeMillis() - startTime);
        }
    }
}