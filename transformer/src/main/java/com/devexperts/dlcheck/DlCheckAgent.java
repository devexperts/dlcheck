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

import com.devexperts.jagent.CachingClassFileTransformer;
import com.devexperts.jagent.JAgent;
import com.devexperts.jagent.JAgentUtil;
import com.devexperts.jagent.Log;
import org.aeonbits.owner.ConfigFactory;

import java.lang.instrument.Instrumentation;

@SuppressWarnings("unused") // uses under DlCheckAgentRunner
public class DlCheckAgent extends JAgent {
    private DlCheckAgent(Instrumentation inst, String args, String agentName, String agentVersion, Log log) {
        super(inst, agentName, agentVersion, log);
    }

    public static DlCheckAgent create(Instrumentation inst, String args) {
        String agentName = JAgentUtil.getImplTitle(DlCheckAgent.class);
        String agentVersion = JAgentUtil.getImplVersion(DlCheckAgent.class);
        Configuration cfg = ConfigFactory.create(Configuration.class, System.getProperties());
        Log.Level logLevel;
        try {
            logLevel = Log.Level.valueOf(cfg.logLevel());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level: " + cfg.logLevel() + ", INFO used by default");
            logLevel = Log.Level.INFO;
        }
        Log log = new Log(agentName, logLevel, cfg.logFile());
        DlCheckAgent agent = new DlCheckAgent(inst, args, agentName, agentVersion, log);
        agent.setRedefineClasses(cfg.redefine());
        agent.setIsVerboseRedefinition(cfg.verboseRedifinition());
        CachingClassFileTransformer transformer = new Transformer(cfg, log, agentVersion);
        transformer.setCacheDir(cfg.cacheDir());
        transformer.setDumpDir(cfg.dumpDir());
        agent.addTransformer(transformer);
        LocationManager.getInstance().init(cfg.cacheDir(), agentVersion);
        return agent;
    }
}
