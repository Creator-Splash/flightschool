package com.a3v1k.flightSchool.application.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstraction for scheduling tasks within game runtime
 *
 * <p>Decouples application logic from platform scheduling system (e.g, Bukkit, Paper, Folia)</p>
 *
 * @author Cammy
 */
public interface Scheduler {

    void onDisable();

    /**
     * @return an executor bound to the main thread
     */
    Executor syncExecutor();

    /**
     * @return an executor for asynchronous execution
     */
    Executor asyncExecutor();

    /**
     * Runs a task synchronously on the main thread as soon as possible
     *
     * @param task the task to execute
     * @return a handle that can be used to cancel the task
     */
    Task run(Runnable task);

    /**
     * Runs a task asynchronously
     *
     * <p>Implementations must ensure that the task does not interact with
     * thread-unsafe APIs (e.g. most Bukkit/Paper APIs)</p>
     *
     * @param task the task to execute
     * @return a handle that can be used to cancel the task
     */
    Task runAsync(Runnable task);

    /**
     * Runs a task synchronously after a delay
     *
     * @param task the task to execute
     * @param delayTicks the delay before execution, in ticks
     * @return a handle that can be used to cancel the task
     */
    Task runLater(Runnable task, long delayTicks);

    /**
     * Runs a task asynchronously after a delay
     *
     * @param task the task to execute
     * @param delayTicks the delay before execution, in ticks
     * @return a handle that can be used to cancel the task
     */
    Task runAsyncLater(Runnable task, long delayTicks);

    /**
     * Runs a task synchronously at a fixed interval
     *
     * @param task the task to execute
     * @param delayTicks the initial delay before first execution, in ticks
     * @param periodTicks the interval between executions, in ticks
     * @return a handle that can be used to cancel the task
     */
    Task runRepeating(Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs a task synchronously at a fixed interval, providing a {@link TickingTask}
     * to the consumer for tick-count access and self-cancellation
     *
     * @param task the task to execute, receives a handle for cancellation and tick tracking
     * @param delayTicks   the initial delay before first execution, in ticks
     * @param periodTicks  the interval between executions, in ticks
     * @return a handle that can be used to cancel the task externally
     */
    Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks);

    /**
     * Runs a task asynchronously at a fixed interval
     *
     * @param task the task to execute
     * @param delayTicks the initial delay before first execution, in ticks
     * @param periodTicks the interval between executions, in ticks
     * @return a handle that can be used to cancel the task
     */
    Task runRepeatingAsync(Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs a task synchronously at a fixed interval for a maximum number of ticks,
     * auto-cancelling after {@code maxTicks} executions
     *
     * @param task the task to execute, receives a handle for cancellation and tick tracking
     * @param delayTicks the initial delay before first execution, in ticks
     * @param periodTicks the interval between executions, in ticks
     * @param maxTicks the maximum number of times the task will execute before auto-cancelling
     * @return a handle that can be used to cancel the task externally before it completes
     */
    Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks, int maxTicks);

    /**
     * Represents a scheduled task
     *
     * <p>This handle allows the caller to cancel a previously scheduled task.
     * Implementations should ensure that cancellation is safe to call multiple times</p>
     */
    interface Task {

        /**
         * Cancels the scheduled task
         */
        void cancel();
    }

    /* Utils */

    /**
     * Executes a computation asynchronously, then schedules a synchronous callback
     * with the result on the main thread
     *
     * <p>This is a convenience method for bridging asynchronous work (e.g. database or
     * network operations) back into the main thread for safe interaction with game state.</p>
     *
     * @param asyncTask the asynchronous computation
     * @param syncCallback the callback to run on the main thread with the result
     * @param <T> the result type
     */
    default <T> void supplyAsyncThenSync(
            Supplier<T> asyncTask,
            Consumer<T> syncCallback
    ) {
        CompletableFuture
                .supplyAsync(asyncTask, asyncExecutor())
                .thenAccept(result -> run(() -> syncCallback.accept(result)));
    }

    /**
     * Executes a computation asynchronously, then schedules a synchronous callback
     * with the result on the main thread, with error handling
     *
     * <p>If the asynchronous task throws an exception, the error handler is invoked
     * on the main thread</p>
     *
     * <p>This method simplifies common async-to-sync workflows while ensuring
     * thread safety for game state interactions</p>
     *
     * @param asyncTask the asynchronous computation
     * @param syncCallback the callback to run on success
     * @param onError the callback to run if an error occurs
     * @param <T> the result type
     */
    default <T> void supplyAsyncThenSync(
            Supplier<T> asyncTask,
            Consumer<T> syncCallback,
            Consumer<Throwable> onError
    ) {
        CompletableFuture
                .supplyAsync(asyncTask, asyncExecutor())
                .whenComplete((result, error) -> {
                    if (error != null) {
                        run(() -> onError.accept(error));
                    } else {
                        run(() -> syncCallback.accept(result));
                    }
                });
    }

}

