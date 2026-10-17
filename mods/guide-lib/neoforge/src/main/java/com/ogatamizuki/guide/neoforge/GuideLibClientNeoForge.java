package com.ogatamizuki.guide.neoforge;

import com.ogatamizuki.guide.GuideBookLoader;
import com.ogatamizuki.guide.GuideManualLoader;
import com.ogatamizuki.guide.GuideThemeLoader;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.GuideRecipeCache;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class GuideLibClientNeoForge {
    private GuideLibClientNeoForge() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(GuideLibClientNeoForge::onAddClientReloadListeners);
        NeoForge.EVENT_BUS.addListener(GuideLibClientNeoForge::onClientResourceLoadFinished);
        NeoForge.EVENT_BUS.addListener(GuideLibClientNeoForge::onClientLoggingIn);
    }

    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(GuideBookLoader.LISTENER_ID, new GuideBookLoader());
        event.addListener(GuideThemeLoader.LISTENER_ID, new GuideThemeLoader());
        event.addListener(GuideManualLoader.LISTENER_ID, new GuideManualLoader());
        // RecipeManager is no longer a PreparableReloadListener in 26.3; wrap recreation on reload.
        event.addListener(GuideRecipeCache.LISTENER_ID, new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(
                    SharedState sharedState,
                    Executor backgroundExecutor,
                    PreparationBarrier preparationBarrier,
                    Executor gameExecutor
            ) {
                return preparationBarrier.wait(null).thenRunAsync(GuideRecipeCache::createRecipeManager, gameExecutor);
            }
        });
    }

    private static void onClientResourceLoadFinished(ClientResourceLoadFinishedEvent event) {
        GuideLibClient.onClientResourceReload();
    }

    private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        GuideLibClient.onClientLoggingIn();
    }
}
