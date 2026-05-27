package com.a3v1k.flightSchool.platform.paper.integration.scheduler;

import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.application.scheduler.TickingTask;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class PaperSchedulerAdapter implements Scheduler {

    private final FlightSchool plugin;

    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    private BukkitScheduler scheduler() {
        return plugin.getServer().getScheduler();
    }

    @Override
    public void onDisable() {
        asyncExecutor.shutdown();
    }

    @Override
    public Executor syncExecutor() {
        return scheduler().getMainThreadExecutor(plugin);
    }

    @Override
    public Executor asyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public Task run(Runnable task) {
        BukkitTask bukkitTask = scheduler().runTask(plugin, task);
        return bukkitTask::cancel;
    }

    @Override
    public Task runAsync(Runnable task) {
        BukkitTask bukkitTask = scheduler().runTaskAsynchronously(plugin, task);
        return bukkitTask::cancel;
    }

    @Override
    public Task runLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runAsyncLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    @Override
    public Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks) {
        MutableTickingTask tickingTask = new MutableTickingTask();

        BukkitTask bukkitTask = scheduler().runTaskTimer(plugin, () -> {
            task.accept(tickingTask);
            tickingTask.increment();
        }, delayTicks, periodTicks);

        tickingTask.setHandle(bukkitTask);
        return tickingTask;
    }

    @Override
    public Task runRepeating(Consumer<TickingTask> task, long delayTicks, long periodTicks, int maxTicks) {
        MutableTickingTask tickingTask = new MutableTickingTask();

        BukkitTask bukkitTask = scheduler().runTaskTimer(plugin, () -> {
            task.accept(tickingTask);
            tickingTask.increment();
            if (tickingTask.elapsedTicks() >= maxTicks) {
                tickingTask.cancel();
            }
        }, delayTicks, periodTicks);

        tickingTask.setHandle(bukkitTask);
        return tickingTask;
    }

    @Override
    public Task runRepeatingAsync(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    private static final class MutableTickingTask implements TickingTask {
        private int ticks = 0;
        private BukkitTask handle;

        void increment() { ticks++; }
        void setHandle(BukkitTask handle) { this.handle = handle; }

        @Override public int elapsedTicks() { return ticks; }
        @Override public void cancel() { if (handle != null) handle.cancel(); }
    }

}
