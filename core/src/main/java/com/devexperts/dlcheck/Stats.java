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

import com.devexperts.dlcheck.api.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

public class Stats {
    public static final String GRAPH_DUMP_EXTENSION = ".dot";
    private static final String STATS_EXTENSION = ".stats";

    private static final String STATS_DIR = System.getProperty("dlcheck.stats.dir");
    public static final boolean STATS_ENABLED = STATS_DIR != null;

    private final AtomicLong monitor_enters = new AtomicLong();
    private final AtomicLong monitor_enters_reentrant = new AtomicLong();
    private final AtomicLong monitor_enters_time = new AtomicLong();
    private final AtomicLong gets_owner_time = new AtomicLong();
    private final AtomicLong adds_adged_time = new AtomicLong();

    private final AtomicLong new_lock_nodes = new AtomicLong();
    private final AtomicLong new_lock_nodes_time = new AtomicLong();

    private final AtomicLong maintains_top_order = new AtomicLong();
    private final AtomicLong maintains_top_order_time = new AtomicLong();

    private final AtomicLong cleans_up_buffer_from_create_node = new AtomicLong();
    private final AtomicLong cleans_up_buffer_from_create_node_time = new AtomicLong();

    private final AtomicLong monitor_exits_from_top = new AtomicLong();

    private final AtomicLong monitor_enters_on_object = new AtomicLong();
    private final AtomicLong monitor_enters_on_lock_node_holder = new AtomicLong();
    private final AtomicLong monitor_enters_on_lock = new AtomicLong();
    private final AtomicLong monitor_enters_on_custom_object = new AtomicLong();

    private final AtomicLong edge_adds_new = new AtomicLong();
    private final AtomicLong edge_adds_same = new AtomicLong();
    private final AtomicLong edge_adds_cyclic = new AtomicLong();

    private volatile long cycles;
    private volatile long cycles_unique;
    private volatile long potential_deadlocks;

    private final AtomicIntegerArray lockStackSize = new AtomicIntegerArray(200);
//    private final AtomicIntegerArray maintainTopOrder = new AtomicIntegerArray(200);

    public void inc_monitor_enters() {
        if (STATS_ENABLED) {
            monitor_enters.incrementAndGet();
        }
    }

    public void inc_monitor_enters_reentrant() {
        if (STATS_ENABLED) {
            monitor_enters_reentrant.incrementAndGet();
        }
    }

    public void increase_monitor_enters_time(long timeNanos) {
        if (STATS_ENABLED) {
            monitor_enters_time.addAndGet(timeNanos);
        }
    }

    public void increase_gets_owners_time(long timeNanos) {
        if (STATS_ENABLED) {
            gets_owner_time.addAndGet(timeNanos);
        }
    }

    public void increase_adds_adges_time(long timeNanos) {
        if (STATS_ENABLED) {
            adds_adged_time.addAndGet(timeNanos);
        }
    }




    public void inc_monitor_exits_from_top() {
        if (STATS_ENABLED) {
            monitor_exits_from_top.incrementAndGet();
        }
    }




    public void inc_monitor_enters_on_object() {
        if (STATS_ENABLED) {
            monitor_enters_on_object.incrementAndGet();
        }
    }

    public void inc_monitor_enters_on_lock() {
        if (STATS_ENABLED) {
            monitor_enters_on_lock.incrementAndGet();
        }
    }

    public void inc_monitor_enters_on_lock_node_holder() {
        if (STATS_ENABLED) {
            monitor_enters_on_lock_node_holder.incrementAndGet();
        }
    }

    public void inc_monitor_enters_on_custom_object() {
        if (STATS_ENABLED) {
            monitor_enters_on_custom_object.incrementAndGet();
        }
    }




    public void inc_new_lock_nodes() {
        if (STATS_ENABLED) {
            new_lock_nodes.incrementAndGet();
        }
    }

    public void increase_new_lock_nodes_time(long timeNanos) {
        if (STATS_ENABLED) {
            new_lock_nodes_time.getAndAdd(timeNanos);
        }
    }




    public void inc_cleans_up_buffer_from_creae_node() {
        if (STATS_ENABLED) {
            cleans_up_buffer_from_create_node.incrementAndGet();
        }
    }

    public void increase_cleans_up_buffer_from_creae_node(long timeNanos) {
        if (STATS_ENABLED) {
             cleans_up_buffer_from_create_node_time.getAndAdd(timeNanos);
        }
    }




    public void inc_maintains_top_order() {
        if (STATS_ENABLED) {
            maintains_top_order.incrementAndGet();
        }
    }

    public void increase_maintains_top_order_time(long timeNanos) {
        if (STATS_ENABLED) {
            maintains_top_order_time.getAndAdd(timeNanos);
        }
    }




    public void inc_edge_adds_new() {
        if (STATS_ENABLED) {
            edge_adds_new.incrementAndGet();
        }
    }

    public void inc_edge_adds_same() {
        if (STATS_ENABLED) {
            edge_adds_same.incrementAndGet();
        }
    }

    public void inc_edge_adds_cyclic() {
        if (STATS_ENABLED) {
            edge_adds_cyclic.incrementAndGet();
        }
    }




    public void inc_cycles() {
        if (STATS_ENABLED) {
            cycles++;
        }
    }

    public void inc_cycles_unique() {
        if (STATS_ENABLED) {
            cycles_unique++;
        }
    }

    public void inc_potential_deadlocks() {
        if (STATS_ENABLED) {
            potential_deadlocks++;
        }
    }




    public void inc_lock_stack_size_usage(int i) {
        if (STATS_ENABLED) {
            if (i >= lockStackSize.length())
                i = lockStackSize.length() - 1;
            lockStackSize.incrementAndGet(i);
        }
    }

//    public void inc_maintain_top_order(int i) {
//        if (STATS_ENABLED) {
//            if (i >= maintainTopOrder.length())
//                i = maintainTopOrder.length() - 1;
//            maintainTopOrder.incrementAndGet(i);
//        }
//    }

    public void dump() {
        try (PrintWriter pw = new PrintWriter(createStatsFilePath(STATS_EXTENSION).toFile())) {
            printKeyValue(pw, "monitor_enters", monitor_enters);
            printKeyValue(pw, "monitor_enters_reentrant", monitor_enters_reentrant);
            printKeyValue(pw, "monitor_enter_time", (monitor_enters_time.get() / monitor_enters.get()) + "ns");
            printKeyValue(pw, "get_owner_time", time(gets_owner_time.get(), monitor_enters.get()) + "ns");
            printKeyValue(pw, "add_edges_time", time(adds_adged_time.get(), monitor_enters.get()) + "ns");

            printKeyValue(pw, "monitor_exits_from_top", monitor_exits_from_top.get());

            printKeyValue(pw, "new_lock_nodes", new_lock_nodes.get());
            printKeyValue(pw, "new_lock_node_time", time(new_lock_nodes_time.get(), new_lock_nodes.get()) + "ns");

            printKeyValue(pw, "cleans_up_buffer_from_create_node", cleans_up_buffer_from_create_node.get());
            printKeyValue(pw, "clean_up_buffer_from_create_node_time", time(cleans_up_buffer_from_create_node_time.get(), cleans_up_buffer_from_create_node.get()) + "ns");

            printKeyValue(pw, "maintains_top_order", maintains_top_order.get());
            printKeyValue(pw, "maintain_top_order_time", time(maintains_top_order_time.get(), maintains_top_order.get()) + "ns");

            printKeyValue(pw, "monitor_enters_on_object", monitor_enters_on_object.get());
            printKeyValue(pw, "monitor_enters_on_lock_node_holder", monitor_enters_on_lock_node_holder.get());
            printKeyValue(pw, "monitor_enters_on_lock", monitor_enters_on_lock.get());
            printKeyValue(pw, "monitor_enters_on_custom_object", monitor_enters_on_custom_object.get());

            printKeyValue(pw, "edge_adds_new", edge_adds_new);
            printKeyValue(pw, "edge_adds_same", edge_adds_same);
            printKeyValue(pw, "edge_adds_cyclic", edge_adds_cyclic);

            printKeyValue(pw, "cycles", cycles);
            printKeyValue(pw, "cycles_unique", cycles_unique);
            printKeyValue(pw, "potential_deadlocks", potential_deadlocks);

            for (int i = 0; i < lockStackSize.length(); i++) {
                int val = lockStackSize.get(i);
                if (val != 0)
                    printKeyValue(pw, "lock_stack_size[" + i + "]", val);
            }

//            for (int i = 0; i < maintainTopOrder.length(); i++) {
//                int val = maintainTopOrder.get(i);
//                if (val != 0)
//                    printKeyValue(pw, "maintain_top_order[" + i + "]", val);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long time(long timeTotal, long count) {
        if (count == 0)
            return -1;
        return timeTotal / count;
    }

    private static final int MAX_KEY_LENGTH = 40;

    private void printKeyValue(PrintWriter pw, String key, Object value) {
        if (key.length() <= MAX_KEY_LENGTH) {
            pw.print(key);
            pw.print(' ');
            for (int i = 0; i < MAX_KEY_LENGTH - key.length(); i++)
                pw.print('.');
            pw.print(' ');
            pw.println(value);
        }
    }

    static Path createStatsFilePath(String extension) throws IOException {
        Path p = Paths.get(STATS_DIR, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + extension);
        FileUtils.createMissingDirectories(p);
        return p;
    }
}
