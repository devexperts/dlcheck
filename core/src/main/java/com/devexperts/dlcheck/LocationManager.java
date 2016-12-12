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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardOpenOption.APPEND;

public class LocationManager {
    private static final String LOCATIONS_CACHE = "locations.cache";

    private static final LocationManager INSTANCE = new LocationManager();
    private final ArrayList<StackTraceElement> locations = new ArrayList<>(10_000);
    private final Map<StackTraceElement, Integer> locationIds = new ConcurrentHashMap<>();
    private volatile BufferedWriter cacheWriter = null;

    public static LocationManager getInstance() {
        return INSTANCE;
    }

    private LocationManager() {
        locations.add(null);
    }

    public synchronized void init(String cacheDir, String agentVersion) {
        if (cacheDir == null)
            return;
        Path locationCachePath = Paths.get(cacheDir, agentVersion, LOCATIONS_CACHE);
        boolean locationCacheExists = Files.exists(locationCachePath);
        if (locationCacheExists) {
            try (BufferedReader br = Files.newBufferedReader(locationCachePath)) {
                while (br.ready()) {
                    String[] arr = br.readLine().split(" ");
                    if (arr.length == 0)
                        break;
                    int id = locations.size();
                    StackTraceElement location = new StackTraceElement(arr[0], arr[1], arr[2], Integer.valueOf(arr[3]));
                    locations.add(location);
                    locationIds.put(location, id);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read locations from " + locationCachePath, e);
            }
        }
        try {
            if (locationCacheExists) {
                cacheWriter = Files.newBufferedWriter(locationCachePath, APPEND);
            } else {
                Files.createDirectories(locationCachePath.getParent());
                cacheWriter = Files.newBufferedWriter(locationCachePath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open " + locationCachePath + " for writing cache", e);
        }
    }

    public synchronized int getLocationId(String className, String methodName, String fileName, int line) {
        StackTraceElement location = new StackTraceElement(className, methodName, fileName, line);
        Integer id = locationIds.get(location);
        if (id != null)
            return id;
        id = locations.size();
        locations.add(location);
        locationIds.put(location, id);
        cacheLocation(location);
        return id;
    }

    private void cacheLocation(StackTraceElement location) {
        if (cacheWriter == null)
            return;
        try {
            cacheWriter.write(location.getClassName());
            cacheWriter.write(' ');
            cacheWriter.write(location.getMethodName());
            cacheWriter.write(' ');
            cacheWriter.write(location.getFileName());
            cacheWriter.write(' ');
            cacheWriter.write("" + location.getLineNumber());
            cacheWriter.newLine();
            cacheWriter.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write location cache", e);
        }
    }

    public synchronized StackTraceElement getLocation(int id) {
        return locations.get(id);
    }
}