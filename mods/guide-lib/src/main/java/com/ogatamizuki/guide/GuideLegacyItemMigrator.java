package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.function.Function;

public final class GuideLegacyItemMigrator {
    private static final Identifier LEGACY_CODEX = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex");
    private static final Identifier LEGACY_CODEX_TABLET = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex_tablet");
    private static final Identifier LEGACY_UNCRAFTER_MANUAL = Identifier.fromNamespaceAndPath("uncrafter", "manual");

    private GuideLegacyItemMigrator() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        migrateInventory(player.getInventory());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        event.getChunk().getBlockEntities().values().forEach(GuideLegacyItemMigrator::migrateBlockEntity);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            migrateInventory(player.getInventory());
        }
    }

    private static void migrateBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            migrateContainer(container);
        }
    }

    private static void migrateContainer(net.minecraft.world.Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            ItemStack migrated = migrateStack(stack);
            if (migrated != stack) {
                container.setItem(slot, migrated);
            }
        }
    }

    private static void migrateInventory(net.minecraft.world.entity.player.Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            ItemStack migrated = migrateStack(stack);
            if (migrated != stack) {
                inventory.setItem(slot, migrated);
            }
        }
    }

    public static ItemStack migrateStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Function<ItemStack, ItemStack> migration = migrationFor(itemId);
        if (migration != null) {
            ItemStack migrated = migration.apply(stack);
            migrated.setCount(stack.getCount());
            stack = migrated;
        }
        return GuideItems.applyItemModel(stack);
    }

    private static Function<ItemStack, ItemStack> migrationFor(Identifier itemId) {
        if (LEGACY_CODEX.equals(itemId)) {
            return ignored -> GuideItems.createCodex(GuideTheme.BOOK_ID);
        }
        if (LEGACY_CODEX_TABLET.equals(itemId)) {
            return ignored -> GuideItems.createCodexTablet();
        }
        if (LEGACY_UNCRAFTER_MANUAL.equals(itemId)) {
            return ignored -> GuideItems.createManual(
                    Identifier.fromNamespaceAndPath("deconstructor", "deconstructor"),
                    "item.deconstructor.manual",
                    GuideTheme.BOOK_ID
            );
        }
        return null;
    }
}
