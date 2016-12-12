package com.devexperts.dlcheck.api;

/*
 * #%L
 * api
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

import java.io.PrintStream;
import java.util.Arrays;

class PrintUtils {

    private PrintUtils() {
    }

    static void printStacktrace(PrintStream out, Iterable<StackTraceElement> stackTrace) {
        for (StackTraceElement se : stackTrace) {
            out.print('\t');
            out.println(se);
        }
    }

    static void printAsBigHeader(PrintStream out, String s) {
        char[] cs = new char[s.length()];
        Arrays.fill(cs, '=');
        String line = String.valueOf(cs);
        out.println(line);
        out.println(s);
        out.println(line);
    }

    static void printAsMediumHeader(PrintStream out, String s) {
        char[] cs = new char[s.length()];
        Arrays.fill(cs, '-');
        String line = String.valueOf(cs);
        out.println(line);
        out.println(s);
        out.println(line);
    }

    static void printAsSmallHeader(PrintStream out, String s) {
        out.print("### ");
        out.print(s);
        out.println(" ###");
    }
}
