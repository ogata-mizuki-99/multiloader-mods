package com.ogatamizuki.elytraslot.neoforge.client;

import com.ogatamizuki.elytraslot.Config;
import com.ogatamizuki.elytraslot.CustomSlotVisibility;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import com.ogatamizuki.elytraslot.client.CustomAttachmentSlots;
import com.ogatamizuki.elytraslot.client.ElytraHudRenderer;
import com.ogatamizuki.elytraslot.client.KeyMappings;
import com.ogatamizuki.elytraslot.client.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.resources.Identifier;

public class ElytraSlotModNeoForgeClient {

    public static void init(ModContainer container) {
        ElytraSlotCommon.sendToServer = payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(payload);
            }
        };
        CustomSlotVisibility.setCheck(CustomAttachmentSlots::shouldRender);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigScreen(parent));
        container.getEventBus().addListener(ElytraSlotModNeoForgeClient::onRegisterGuiLayers);
        container.getEventBus().addListener(ElytraSlotModNeoForgeClient::onRegisterKeyMappings);
        container.getEventBus().addListener(ElytraSlotModNeoForgeClient::onConfigLoad);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> KeyMappings.tick());
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (!"elytra_slot-client.toml".equals(event.getConfig().getFileName())) {
            return;
        }
        migrateLegacyCreativeSlotPositions();
        migrateLegacyHudPositions();
    }

    /** Old creative defaults overlapped the player preview; bump saved values once. */
    private static void migrateLegacyCreativeSlotPositions() {
        if (Config.CREATIVE_ELYTRA_SLOT_X.get() == 116
                && Config.CREATIVE_ELYTRA_SLOT_Y.get() == 26
                && Config.CREATIVE_FIREWORK_SLOT_X.get() == 116
                && Config.CREATIVE_FIREWORK_SLOT_Y.get() == 8) {
            Config.CREATIVE_ELYTRA_SLOT_X.set(126);
            Config.CREATIVE_ELYTRA_SLOT_Y.set(33);
            Config.CREATIVE_FIREWORK_SLOT_X.set(126);
            Config.CREATIVE_FIREWORK_SLOT_Y.set(6);
            Config.CREATIVE_ELYTRA_SLOT_X.save();
            Config.CREATIVE_ELYTRA_SLOT_Y.save();
            Config.CREATIVE_FIREWORK_SLOT_X.save();
            Config.CREATIVE_FIREWORK_SLOT_Y.save();
        }
    }

    /** Old HUD defaults overlapped the offhand slot; bump saved values once. */
    private static void migrateLegacyHudPositions() {
        if (Config.ELYTRA_HUD_X.get() == -120
                && Config.ELYTRA_HUD_Y.get() == -22
                && Config.FIREWORK_HUD_X.get() == -140
                && Config.FIREWORK_HUD_Y.get() == -22) {
            Config.ELYTRA_HUD_X.set(-150);
            Config.ELYTRA_HUD_Y.set(-22);
            Config.FIREWORK_HUD_X.set(-170);
            Config.FIREWORK_HUD_Y.set(-22);
            Config.ELYTRA_HUD_X.save();
            Config.ELYTRA_HUD_Y.save();
            Config.FIREWORK_HUD_X.save();
            Config.FIREWORK_HUD_Y.save();
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath("elytra_slot", "elytra_hud"),
                ElytraHudRenderer::render
        );
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyMappings.CATEGORY);
        event.register(KeyMappings.QUICK_SWAP_KEY);
        event.register(KeyMappings.FIREWORK_BOOST_KEY);
    }
}
