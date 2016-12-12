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

import java.util.regex.Pattern;

/**
 * Utility methods to support simple glob patterns:
 * <ul>
 * <li>Use '*' for any sequence of characters (regex equivalent is '.*')
 * <li>Use ',' for a list of choices (regex equivalent is '|')
 * </ul>
 */
public class GlobUtil {
    private GlobUtil() {
    } // to prevent inheritance and construction of the class

    public static Pattern compile(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case ',':
                    regex.append('|');
                    break;
                default:
                    regex.append(Pattern.quote(glob.substring(i, i + 1)));
            }
        }
        return Pattern.compile(regex.toString());
    }
}
