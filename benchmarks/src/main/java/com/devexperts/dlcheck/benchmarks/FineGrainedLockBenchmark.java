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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10)
@Fork(1)
@Measurement(iterations = 10, time = 5)
@State(Scope.Benchmark)
public class FineGrainedLockBenchmark {

    @Param({"10", "100", "1000", "10000", "100000", "1000000"})
    private int n;
    @Param({"2", "3", "4"})
    private int depth;

    private Lock[] locks;

    @Setup(Level.Iteration)
    public void setup() {
        locks = new ReentrantLock[n];
        for (int i = 0; i < n; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public void operation(FineGrainedLockBenchmark state) {
        Random r = ThreadLocalRandom.current();
        int[] ids = new int[depth];
        for (int i = 0; i < depth; i++) {
            ids[i] = r.nextInt(n);
        }
        for (int i = 0; i < ids.length; i++) {
            ids[i] = r.nextInt(n);
        }
        Arrays.sort(ids);
        for (int i = 0; i < ids.length; i++) {
            locks[ids[i]].lock();
        }
        Blackhole.consumeCPU(10); // do smth
        for (int i = ids.length - 1; i >= 0; i--) {
            locks[ids[i]].unlock();
        }
    }

    @Benchmark
    @Threads(1)
    public void bench_1(FineGrainedLockBenchmark state) {
        operation(state);
    }

    @Benchmark
    @Threads(2)
    public void bench_2(FineGrainedLockBenchmark state) {
        operation(state);
    }

    @Benchmark
    @Threads(4)
    public void bench_4(FineGrainedLockBenchmark state) {
        operation(state);
    }

    @Benchmark
    @Threads(8)
    public void bench_8(FineGrainedLockBenchmark state) {
        operation(state);
    }

    @Benchmark
    @Threads(16)
    public void bench_16(FineGrainedLockBenchmark state) {
        operation(state);
    }

    @Benchmark
    @Threads(32)
    public void bench_32(FineGrainedLockBenchmark state) {
        operation(state);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(FineGrainedLockBenchmark.class.getSimpleName())
            .resultFormat(ResultFormatType.CSV)
            .build();
        new Runner(opt).run();
    }
}
