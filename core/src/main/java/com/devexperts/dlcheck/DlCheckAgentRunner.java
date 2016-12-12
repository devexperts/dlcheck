package com.devexperts.dlcheck;

/*
 * #%L
 * core
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

import com.devexperts.jagent.InnerJarClassLoader;
import com.devexperts.jagent.JAgentRunner;

import java.lang.instrument.Instrumentation;

public class DlCheckAgentRunner {
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        JAgentRunner.runAgent("com.devexperts.dlcheck.DlCheckAgent", inst, agentArgs,
                InnerJarClassLoader.createForJars(
                        "asm-all.jar",
                        "jagent-impl.jar",
                        "transformer.jar",
                        "owner.jar",
                        "owner-java8.jar"
                ));
    }
}
