package com.example.ridematching.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

public final class Race {

    private static final long TIMEOUT_SECONDS = 20;

    private Race() {
    }

    /** Runs {@code contenders} copies of {@code attempt}; each gets its own index 0..n-1. */
    public static <T> Result<T> run(int contenders, IntFunction<T> attempt) {
        List<Callable<T>> tasks = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            int index = i;
            tasks.add(() -> attempt.apply(index));
        }
        return run(tasks);
    }

    public static <T> Result<T> run(List<Callable<T>> tasks) {
        Queue<T> winners = new ConcurrentLinkedQueue<>();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        CountDownLatch startingGun = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());

        for (Callable<T> task : tasks) {
            pool.execute(() -> {
                try {
                    startingGun.await();
                    winners.add(task.call());
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
        }
        startingGun.countDown();
        awaitFinish(pool);
        return new Result<>(List.copyOf(winners), List.copyOf(failures));
    }

    private static void awaitFinish(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                throw new AssertionError("Race did not finish within " + TIMEOUT_SECONDS + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for race", e);
        }
    }

    /** What the contenders returned (winners) and what they threw (failures). */
    public record Result<T>(List<T> winners, List<Throwable> failures) {
    }
}
