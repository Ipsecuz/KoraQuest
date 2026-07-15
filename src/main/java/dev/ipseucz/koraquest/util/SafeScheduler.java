package dev.ipseucz.koraquest.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class SafeScheduler {
    private final Plugin plugin;
    private final boolean folia;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final List<Object> handles = new CopyOnWriteArrayList<>();

    public SafeScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = PlatformDetector.isFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public void runGlobal(Runnable runnable) {
        if (!canRun(runnable)) {
            return;
        }
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class).invoke(scheduler, plugin, runnable);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("global execute", exception);
                return;
            }
        }
        addHandle(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    public void runGlobalLater(Runnable runnable, long delayTicks) {
        if (!canRun(runnable)) {
            return;
        }
        long delay = Math.max(1L, delayTicks);
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                Object handle = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run(), delay);
                addHandle(handle);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("global delayed task", exception);
                return;
            }
        }
        addHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public void runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!canRun(runnable)) {
            return;
        }
        long delay = Math.max(1L, delayTicks);
        long period = Math.max(1L, periodTicks);
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object handle = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run(), delay, period);
                addHandle(handle);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("global repeating task", exception);
                return;
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        addHandle(task);
    }

    public void runEntity(Player player, Runnable runnable) {
        runEntityLater(player, runnable, 1L);
    }

    public void runEntityLater(Player player, Runnable runnable, long delayTicks) {
        if (player == null || !canRun(runnable)) {
            return;
        }
        long delay = Math.max(1L, delayTicks);
        if (folia) {
            try {
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                Object handle = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run(), null, delay);
                addHandle(handle);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("entity task for " + player.getName(), exception);
                return;
            }
        }
        addHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    public void runAsync(Runnable runnable) {
        if (!canRun(runnable)) {
            return;
        }
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method method = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                Object handle = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run());
                addHandle(handle);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("async task", exception);
                return;
            }
        }
        addHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    public void runAsyncLater(Runnable runnable, long delay, TimeUnit unit) {
        if (!canRun(runnable)) {
            return;
        }
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method method = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                Object handle = method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run(), Math.max(0L, delay), unit);
                addHandle(handle);
                return;
            } catch (ReflectiveOperationException exception) {
                logFailure("delayed async task", exception);
                return;
            }
        }
        long ticks = Math.max(1L, unit.toMillis(Math.max(0L, delay)) / 50L);
        addHandle(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticks));
    }

    public void shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        for (Object handle : handles) {
            cancel(handle);
        }
        handles.clear();
        if (folia) {
            cancelFoliaScheduler("getGlobalRegionScheduler");
            cancelFoliaScheduler("getAsyncScheduler");
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    private boolean canRun(Runnable runnable) {
        return runnable != null && !stopped.get() && plugin.isEnabled();
    }

    private void addHandle(Object handle) {
        if (handle == null) {
            return;
        }
        if (stopped.get()) {
            cancel(handle);
            return;
        }
        handles.add(handle);
        // Close the small race where shutdown starts between the stopped check and add().
        if (stopped.get() && handles.remove(handle)) {
            cancel(handle);
        }
    }

    private void cancel(Object handle) {
        try {
            handle.getClass().getMethod("cancel").invoke(handle);
        } catch (ReflectiveOperationException ignored) {
            if (handle instanceof BukkitTask task) {
                task.cancel();
            }
        }
    }

    private void cancelFoliaScheduler(String getter) {
        try {
            Object scheduler = Bukkit.class.getMethod(getter).invoke(null);
            scheduler.getClass().getMethod("cancelTasks", Plugin.class).invoke(scheduler, plugin);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void logFailure(String action, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, "Could not schedule " + action + ". The task was not run to avoid an unsafe thread fallback.", throwable);
    }
}
