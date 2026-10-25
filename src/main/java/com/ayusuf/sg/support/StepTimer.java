package com.ayusuf.sg.support;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Prints how long each step took, e.g. "[step] 11:31:42 email received  +6.2s (12.4s)". One per test thread. */
public final class StepTimer {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final ThreadLocal<long[]> TIMES = ThreadLocal.withInitial(() -> {
        long now = System.nanoTime();
        return new long[]{now, now};
    });

    private StepTimer() {
    }

    public static void start() {
        long now = System.nanoTime();
        TIMES.set(new long[]{now, now});
    }

    public static void step(String name) {
        long[] t = TIMES.get();
        long now = System.nanoTime();
        System.out.printf("[step] %s %-28s +%.1fs (%.1fs)%n", LocalTime.now().format(CLOCK),
                name, (now - t[1]) / 1e9, (now - t[0]) / 1e9);
        t[1] = now;
    }
}
