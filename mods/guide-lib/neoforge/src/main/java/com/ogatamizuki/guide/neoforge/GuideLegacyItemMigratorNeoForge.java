package com.ogatamizuki.guide.neoforge;

import com.ogatamizuki.guide.GuideLegacyItemMigrator;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class GuideLegacyItemMigratorNeoForge {
    private GuideLegacyItemMigratorNeoForge() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GuideLegacyItemMigrator.migrateInventory(player.getInventory());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            event.getChunk().getBlockEntities().values().forEach(GuideLegacyItemMigrator::migrateBlockEntity);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            GuideLegacyItemMigrator.migrateInventory(player.getInventory());
        }
    }
}
