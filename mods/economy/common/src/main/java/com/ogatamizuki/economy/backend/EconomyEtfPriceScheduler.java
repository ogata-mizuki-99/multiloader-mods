package com.ogatamizuki.economy.backend;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ogatamizuki.economy.EconomyCommon;
import com.ogatamizuki.economy.EconomyFeatures;
import com.ogatamizuki.economy.backend.local.EconomyLocalEtfService;
import com.ogatamizuki.economy.master.EconomyMasterData;

import net.minecraft.server.MinecraftServer;

/** ETF ランダムウォークの定期実行。 */
public final class EconomyEtfPriceScheduler {
    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> task;

    private EconomyEtfPriceScheduler() {
    }

    public static void start(MinecraftServer server) {
        stop();
        if (!EconomyFeatures.isEtfUpdatesEnabled()) {
            EconomyCommon.LOGGER.info("ETF random walk scheduler skipped (enableEtfUpdates=false)");
            return;
        }
        int intervalMinutes = EconomyMasterData.get().etfRandomWalkIntervalMinutes();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "economy-etf-random-walk");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!EconomyFeatures.isEtfUpdatesEnabled()) {
                    return;
                }
                server.execute(EconomyLocalEtfService::applyRandomWalk);
            } catch (Exception e) {
                EconomyCommon.LOGGER.warn("ETF random walk task failed: {}", e.getMessage());
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
        EconomyCommon.LOGGER.info("ETF random walk scheduled every {} minutes", intervalMinutes);
    }

    public static void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
