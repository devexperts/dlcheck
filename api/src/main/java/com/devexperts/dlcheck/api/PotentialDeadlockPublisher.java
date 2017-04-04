package com.devexperts.dlcheck.api;

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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.APPEND;

public class PotentialDeadlockPublisher {
    private static final String OUTPUT_PROPERTY = "dlcheck.output";
    private static volatile PrintStream out = System.out; // print to console by default

    public static void init() {
        try {
            String file = System.getProperty(OUTPUT_PROPERTY);
            if (file != null) {
                Path filePath = Paths.get(file);
                OutputStream outputStream;
                if (Files.exists(filePath)) {
                    outputStream = Files.newOutputStream(filePath, APPEND);
                } else {
                    FileUtils.createMissingDirectories(filePath);
                    outputStream = Files.newOutputStream(filePath);
                    outputStream.flush();
                }
                out = new PrintStream(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized void publishPotentialDeadlock(PotentialDeadlock potentialDeadlock) {
        potentialDeadlock.print(out);
        out.flush();
    }
}
