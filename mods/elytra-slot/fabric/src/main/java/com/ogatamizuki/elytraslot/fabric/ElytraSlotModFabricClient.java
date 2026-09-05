package com.ogatamizuki.elytraslot.fabric;

import com.ogatamizuki.elytraslot.*;
import com.ogatamizuki.elytraslot.client.CustomAttachmentSlots;
import com.ogatamizuki.elytraslot.client.ElytraHudRenderer;
import com.ogatamizuki.elytraslot.client.KeyMappings;
import com.ogatamizuki.elytraslot.network.SlotPosSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ElytraSlotModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ElytraSlotCommon.LOGGER.info("Elytra Slot Mod (Fabric Client) Initializing...");

        ElytraSlotCommon.sendToServer = ClientPlayNetworking::send;
        ElytraSlotCommon.getElytraItem = FabricClientSlotStorage::getElytra;
        ElytraSlotCommon.setElytraItem = FabricClientSlotStorage::setElytra;
        ElytraSlotCommon.getFireworkItem = FabricClientSlotStorage::getFirework;
        ElytraSlotCommon.setFireworkItem = FabricClientSlotStorage::setFirework;
        ElytraSlotCommon.getSlotPositions = FabricClientSlotStorage::getPositions;
        ElytraSlotCommon.setSlotPositions = FabricClientSlotStorage::setPositions;
        CustomSlotVisibility.setCheck(CustomAttachmentSlots::shouldRender);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(KeyMappings.QUICK_SWAP_KEY);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(KeyMappings.FIREWORK_BOOST_KEY);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> KeyMappings.tick());

        // HUD Elements
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                ElytraSlotCommon.id("elytra_hud"),
                ElytraHudRenderer::render
        );

        // Client Network Handlers
        ClientPlayNetworking.registerGlobalReceiver(ElytraSlotSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level != null) {
                    Player target = context.client().level.getPlayerByUUID(payload.playerUuid());
                    if (target != null) {
                        FabricSlotSyncHelper.applyElytraSync(context.client(), target, payload.elytraItem());
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(FireworkSlotSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level != null) {
                    Player target = context.client().level.getPlayerByUUID(payload.playerUuid());
                    if (target != null) {
                        FabricSlotSyncHelper.applyFireworkSync(context.client(), target, payload.fireworkItem());
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SlotPosSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = context.client();
                if (mc.player != null) {
                    SlotPositions current = ElytraSlotCommon.getPositions(mc.player);
                    SlotPositions updated = new SlotPositions(
                            payload.elytraX(), payload.elytraY(),
                            payload.fireworkX(), payload.fireworkY(),
                            current.creativeElytraX(), current.creativeElytraY(),
                            current.creativeFireworkX(), current.creativeFireworkY()
                    );
                    ElytraSlotCommon.setPositions(mc.player, updated);
                    ElytraSlotCommon.updatePlayerContainerSlotPositions(mc.player, payload.elytraX(), payload.elytraY(), payload.fireworkX(), payload.fireworkY());
                }
            });
        });
    }
}
