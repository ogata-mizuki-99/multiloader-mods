package com.ogatamizuki.radialteleport;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class RadialTeleportCommon {
    public static final String MODID = "radial_teleport";
    public static final Logger LOGGER = LogManager.getLogger("RadialTeleport");

    public static Supplier<TeleportCompassItem> TELEPORT_COMPASS;

    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    public static java.util.function.Consumer<CustomPacketPayload> sendToServer = payload -> {};

    public static java.util.function.Predicate<String> isModLoadedCheck = modId -> false;

    private RadialTeleportCommon() {}

    public static boolean isModLoaded(String modId) {
        return isModLoadedCheck.test(modId);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
