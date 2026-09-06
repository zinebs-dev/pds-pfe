// src/main/java/com/pds/pfe/utils/TaskExecutor.java
package com.pds.pfe.ui.utils;

import javafx.concurrent.Task;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {
    private static final ExecutorService backendExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Backend-Thread");
        return thread;
    });

    public static <T> void executeTask(Task<T> task) {
        backendExecutor.execute(task);
    }

    public static void shutdown() {
        backendExecutor.shutdown();
    }
}