package com.devexperts.dlcheck.benchmarks;

/*
 * #%L
 * benchmarks
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5)
@Fork(1)
@Measurement(iterations = 10)
@State(Scope.Benchmark)
public class SynchronizedBenchmark {

    @Param({"Object", "A"})
    String lockClass;

    Object lock;
    long x;

    @Setup
    public void setup() {
        switch (lockClass) {
            case "Object":
                lock = new Object();
                break;
            case "A":
                lock = new A();
                break;
        }
    }

    @Benchmark
    public long measureSynchronized() {
        synchronized (lock) {
            return x++;
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SynchronizedBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    class A {
    }
}
