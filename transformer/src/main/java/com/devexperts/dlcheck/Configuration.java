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

import org.aeonbits.owner.Config;

@Config.Sources("classpath:dlcheck.properties")
public interface Configuration extends Config {

    @DefaultValue("java.util.concurrent.locks.*#lock*")
    @Key("dlcheck.lock.lock")
    String[] lockPatterns(); // TODO javadoc: comma separated

    @DefaultValue("java.util.concurrent.locks.*#tryLock")
    @Key("dlcheck.lock.trylock")
    String[] tryLockPatterns(); // TODO javadoc: comma separated

    @DefaultValue("java.util.concurrent.locks.*#unlock")
    @Key("dlcheck.lock.unlock")
    String[] unlockPatterns(); // TODO javadoc: comma separated



    @Key("dlcheck.log.level")
    @DefaultValue("INFO")
    String logLevel();

    @Key("dlcheck.log.file")
    String logFile();



    @Key("dlcheck.redefinition.verbose")
    @DefaultValue("false")
    boolean verboseRedifinition();

    @Key("dlcheck.redefinition.enabled")
    @DefaultValue("false")
    boolean redefine();



    @Key("dlcheck.cache.dir")
    String cacheDir();

    @Key("dlcheck.dump.dir")
    String dumpDir();



    @Key("dlcheck.include")
    @DefaultValue("*")
    String[] include();

    @Key("dlcheck.exclude")
    @DefaultValue("")
    String[] exclude();
}
