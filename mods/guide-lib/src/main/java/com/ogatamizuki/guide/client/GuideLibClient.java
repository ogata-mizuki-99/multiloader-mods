package com.ogatamizuki.guide.client;

import com.ogatamizuki.guide.GuideBookLoader;
import com.ogatamizuki.guide.GuideDataReloader;
import com.ogatamizuki.guide.GuideManualLoader;
import com.ogatamizuki.guide.GuideThemeLoader;
import com.ogatamizuki.guide.GuideThemeRegistry;
import com.ogatamizuki.guide.client.screen.CodexScreen;
import com.ogatamizuki.guide.client.screen.GuideBookScreen;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class GuideLibClient {
    private GuideLibClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(GuideLibClient::onAddClientReloadListeners);
        NeoForge.EVENT_BUS.addListener(GuideLibClient::onClientResourceLoadFinished);
        NeoForge.EVENT_BUS.addListener(GuideLibClient::onClientLoggingIn);
    }

    private static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(GuideBookLoader.LISTENER_ID, new GuideBookLoader());
        event.addListener(GuideThemeLoader.LISTENER_ID, new GuideThemeLoader());
        event.addListener(GuideManualLoader.LISTENER_ID, new GuideManualLoader());
        GuideRecipeCache.register(event);
    }

    private static void onClientResourceLoadFinished(ClientResourceLoadFinishedEvent event) {
        invalidateModJarCaches();
        ensureDataLoaded();
    }

    private static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(GuideLibClient::ensureDataLoaded);
    }

    public static void openCodex(Screen parent, Identifier bookId) {
        openCodex(parent, bookId, GuideTheme.BOOK_ID);
    }

    public static void openCodex(Screen parent, Identifier bookId, Identifier themeId) {
        ensureDataLoaded();
        GuideTheme theme = GuideThemeRegistry.resolve(themeId);
        playOpenSound(theme);
        Minecraft mc = Minecraft.getInstance();
        if (bookId != null) {
            mc.gui.setScreen(new GuideBookScreen(parent, bookId, theme));
            return;
        }
        mc.gui.setScreen(new CodexScreen(parent, theme));
    }

    public static void openBook(Screen parent, Identifier bookId) {
        openBook(parent, bookId, GuideTheme.BOOK_ID, false);
    }

    public static void openBook(Screen parent, Identifier bookId, Identifier themeId) {
        openBook(parent, bookId, themeId, false);
    }

    public static void openBook(Screen parent, Identifier bookId, Identifier themeId, boolean closeOnBack) {
        ensureDataLoaded();
        GuideTheme theme = GuideThemeRegistry.resolve(themeId);
        playOpenSound(theme);
        Minecraft.getInstance().gui.setScreen(new GuideBookScreen(parent, bookId, theme, closeOnBack));
    }

    public static void ensureDataLoaded() {
        GuideDataReloader.reloadFrom(Minecraft.getInstance().getResourceManager());
        GuideRecipeCache.ensureLoaded();
    }

    private static void invalidateModJarCaches() {
        GuideBookLoader.invalidateModJarCache();
        GuideThemeLoader.invalidateModJarCache();
        GuideManualLoader.invalidateModJarCache();
        GuideRecipeCache.invalidateModJarCache();
    }

    public static void playOpenSound(GuideTheme theme) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && theme != null) {
            GuideClientSounds.play(mc.player, theme.openSound(), 0.8F, 1.0F);
        }
    }

    public static void playPageTurnSound(GuideTheme theme) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && theme != null) {
            GuideClientSounds.play(mc.player, theme.pageTurnSound(), 0.8F, 1.0F);
        }
    }

    public static Component translateOrLiteral(String keyOrText) {
        if (keyOrText == null || keyOrText.isEmpty()) {
            return Component.empty();
        }
        if (keyOrText.startsWith("literal:")) {
            return Component.literal(keyOrText.substring("literal:".length()));
        }
        return Component.translatable(keyOrText);
    }
}
