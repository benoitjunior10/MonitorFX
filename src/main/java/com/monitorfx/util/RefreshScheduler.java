/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.monitorfx.util;

/**
 *
 * @author HP
 * Concurrence et orchestration.
 * Gère les tâches périodiques et les exécutions en arrière-plan.
 */


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshScheduler {

    private final ScheduledExecutorService executorService;

    public RefreshScheduler(int poolSize) {
        this.executorService = Executors.newScheduledThreadPool(poolSize, new MonitorThreadFactory());
    }

    public void execute(Runnable task) {
        executorService.execute(task);
    }

    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        executorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private static final class MonitorThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("monitor-worker-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}

