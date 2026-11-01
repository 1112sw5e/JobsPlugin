package com.tradewind.storage;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Single database thread: serialises all JDBC work away from the server thread. */
public final class DbExecutor implements AutoCloseable {

    private final Logger logger;
    private final ExecutorService executor;

    public DbExecutor(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "Tradewind-DB");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void execute(String description, Runnable task, Runnable onFailure) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException failure) {
                logger.log(Level.SEVERE, "Ошибка БД: " + description, failure);
                if (onFailure != null) {
                    onFailure.run();
                }
            }
        });
    }

    public <T> CompletableFuture<T> supply(String description, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception failure) {
                logger.log(Level.SEVERE, "Ошибка БД: " + description, failure);
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /** Blocks until queued work finishes, used at shutdown. */
    public void drain(long timeoutSeconds) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warning("БД не успела завершить запись за " + timeoutSeconds + " с.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        drain(10L);
    }
}

