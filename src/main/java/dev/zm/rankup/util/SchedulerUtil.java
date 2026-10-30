package dev.zm.rankup.util;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler abstraction that runs the same code on both Paper and Folia.
 *
 * <p>Folia removes the global {@link org.bukkit.scheduler.BukkitScheduler} task methods and the
 * single "main thread" concept, replacing them with region/entity/async schedulers. Because this
 * plugin compiles against Paper 1.19.4 (whose API does not contain those classes), the Folia
 * schedulers are accessed through reflection only when a Folia runtime is detected. On regular
 * Paper nothing changes: tasks route through {@code Bukkit.getScheduler()} and the main thread.</p>
 */
public final class SchedulerUtil {

    private static final boolean FOLIA = detectFolia();

    private static final Method ENTITY_SCHEDULER_GET;
    private static final Method ENTITY_RUN;
    private static final Method ENTITY_RUN_DELAYED;
    private static final Method GLOBAL_RUN;
    private static final Method GLOBAL_RUN_DELAYED;
    private static final Method GLOBAL_RUN_FIXED;
    private static final Method ASYNC_RUN;

    static {
        Method entityGet = null, entityRun = null, entityRunDelayed = null;
        Method globalRun = null, globalRunDelayed = null, globalRunFixed = null;
        Method asyncRun = null;
        if (FOLIA) {
            try {
                Class<?> entitySch = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                Class<?> globalSch = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                Class<?> asyncSch = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");

                entityGet = Entity.class.getMethod("getScheduler");
                entityRun = entitySch.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunDelayed = entitySch.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);

                globalRun = globalSch.getMethod("run", Plugin.class, Consumer.class);
                globalRunDelayed = globalSch.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                globalRunFixed = globalSch.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

                asyncRun = asyncSch.getMethod("runNow", Plugin.class, Consumer.class);
            } catch (Exception ignored) {
                // Reflection setup failed — fall back to Paper scheduling only.
            }
        }
        ENTITY_SCHEDULER_GET = entityGet;
        ENTITY_RUN = entityRun;
        ENTITY_RUN_DELAYED = entityRunDelayed;
        GLOBAL_RUN = globalRun;
        GLOBAL_RUN_DELAYED = globalRunDelayed;
        GLOBAL_RUN_FIXED = globalRunFixed;
        ASYNC_RUN = asyncRun;
    }

    private SchedulerUtil() {
    }

    private static boolean detectFolia() {
        // RegionizedServer only exists on actual Folia (regular Paper 1.20+ ships the
        // scheduler API interfaces but runs them single-threaded, so they must not be
        // treated as Folia here).
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Runs a task on the given entity's thread/region. On Paper this is the main thread (running
     * inline when already there); on Folia it is the entity's scheduler.
     */
    public static void runOnEntity(Plugin plugin, Entity entity, Runnable task) {
        if (entity == null) {
            runGlobal(plugin, task);
            return;
        }
        if (FOLIA && ENTITY_SCHEDULER_GET != null) {
            try {
                Object scheduler = ENTITY_SCHEDULER_GET.invoke(entity);
                ENTITY_RUN.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null);
                return;
            } catch (Exception ignored) {
                // Entity removed or reflection failure — fall through to global.
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task on the given entity's thread after a delay (in ticks).
     */
    public static void runOnEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (entity == null) {
            runGlobalDelayed(plugin, task, delayTicks);
            return;
        }
        if (FOLIA && ENTITY_SCHEDULER_GET != null) {
            try {
                Object scheduler = ENTITY_SCHEDULER_GET.invoke(entity);
                ENTITY_RUN_DELAYED.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null, delayTicks);
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Runs a task on the global region scheduler (Folia) or the main thread (Paper).
     */
    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA && GLOBAL_RUN != null) {
            try {
                Object scheduler = getGlobalScheduler();
                GLOBAL_RUN.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runGlobalDelayed(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA && GLOBAL_RUN_DELAYED != null) {
            try {
                Object scheduler = getGlobalScheduler();
                GLOBAL_RUN_DELAYED.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delayTicks);
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Runs a repeating task (for the auto-rankup loop) on the global region scheduler (Folia) or
     * the main thread (Paper).
     */
    public static void runGlobalTimer(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        if (FOLIA && GLOBAL_RUN_FIXED != null) {
            try {
                Object scheduler = getGlobalScheduler();
                GLOBAL_RUN_FIXED.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), initialDelayTicks,
                        periodTicks);
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
    }

    /**
     * Runs a task asynchronously.
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA && ASYNC_RUN != null) {
            try {
                Object scheduler = getAsyncScheduler();
                ASYNC_RUN.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private static Object getGlobalScheduler() throws Exception {
        Method getter = Server.class.getMethod("getGlobalRegionScheduler");
        return getter.invoke(Bukkit.getServer());
    }

    private static Object getAsyncScheduler() throws Exception {
        Method getter = Server.class.getMethod("getAsyncScheduler");
        return getter.invoke(Bukkit.getServer());
    }
}
