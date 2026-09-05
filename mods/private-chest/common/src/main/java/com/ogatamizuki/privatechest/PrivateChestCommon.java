package com.ogatamizuki.privatechest;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PrivateChestCommon {
    public static final String MODID = "privatechest";
    public static final Logger LOGGER = LogManager.getLogger(PrivateChestCommon.class);

    public static Supplier<LockerBlock> LOCKER_BLOCK;
    public static Supplier<net.minecraft.world.item.BlockItem> LOCKER_BLOCK_ITEM;
    public static Supplier<OwnerPlayerHeadItem> OWNER_PLAYER_HEAD_ITEM;
    public static Supplier<BlockEntityType<LockerBlockEntity>> LOCKER_BLOCK_ENTITY_TYPE;
    public static Supplier<MenuType<LockerMenu>> LOCKER_MENU_TYPE;

    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    public static java.util.function.Consumer<CustomPacketPayload> sendToServer = payload -> {};

    private PrivateChestCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
