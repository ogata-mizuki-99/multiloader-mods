package com.ogatamizuki.privatechest.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ogatamizuki.privatechest.*;
import com.ogatamizuki.privatechest.client.LockerAwareSkullRenderer;
import com.ogatamizuki.privatechest.client.LockerBlockEntityRenderer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class PrivateChestModFabric implements ModInitializer {
    public static final String MODID = "privatechest";
    public static final Logger LOGGER = LogManager.getLogger(PrivateChestModFabric.class);

    public static LockerBlock LOCKER_BLOCK;
    public static BlockItem LOCKER_BLOCK_ITEM;
    public static OwnerPlayerHeadItem OWNER_PLAYER_HEAD_ITEM;
    public static BlockEntityType<LockerBlockEntity> LOCKER_BLOCK_ENTITY_TYPE;
    public static MenuType<LockerMenu> LOCKER_MENU_TYPE;
    public static CreativeModeTab TAB;

    @Override
    public void onInitialize() {
        LOGGER.info("Private Locker Chest Mod (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        // Register Block
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, PrivateChestCommon.id("locker"));
        LOCKER_BLOCK = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new LockerBlock(BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .mapColor(MapColor.WOOD)
                        .strength(2.5F, 1200.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion())
        );

        // Register BlockItem
        ResourceKey<Item> blockItemKey = ResourceKey.create(Registries.ITEM, PrivateChestCommon.id("locker"));
        LOCKER_BLOCK_ITEM = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                blockItemKey,
                new BlockItem(LOCKER_BLOCK, new Item.Properties().setId(blockItemKey))
        );

        // Register OwnerPlayerHeadItem
        ResourceKey<Item> headItemKey = ResourceKey.create(Registries.ITEM, PrivateChestCommon.id("owner_player_head"));
        OWNER_PLAYER_HEAD_ITEM = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                headItemKey,
                new OwnerPlayerHeadItem(new Item.Properties().setId(headItemKey).stacksTo(64).equippableUnswappable(EquipmentSlot.HEAD))
        );

        // Register BlockEntityType
        ResourceKey<BlockEntityType<?>> beKey = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, PrivateChestCommon.id("locker"));
        LOCKER_BLOCK_ENTITY_TYPE = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                beKey,
                FabricRegistryHelper.createBlockEntityType(LockerBlockEntity::new, LOCKER_BLOCK)
        );

        // Register MenuType
        ResourceKey<MenuType<?>> menuKey = ResourceKey.create(Registries.MENU, PrivateChestCommon.id("locker"));
        LOCKER_MENU_TYPE = FabricRegistryHelper.register(
                BuiltInRegistries.MENU,
                menuKey,
                FabricRegistryHelper.createMenuType(
                        (containerId, inv) -> new LockerMenu(containerId, inv, (LockerBlockEntity) null),
                        FeatureFlags.DEFAULT_FLAGS)
        );

        // Register Creative Tab
        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, PrivateChestCommon.id("private_chest_tab"));
        TAB = FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.privatechest"))
                        .icon(() -> LOCKER_BLOCK_ITEM.getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            output.accept(LOCKER_BLOCK_ITEM);
                            output.accept(OWNER_PLAYER_HEAD_ITEM.getDefaultInstance());
                        })
                        .build()
        );

        // Connect Suppliers
        PrivateChestCommon.LOCKER_BLOCK = () -> LOCKER_BLOCK;
        PrivateChestCommon.LOCKER_BLOCK_ITEM = () -> LOCKER_BLOCK_ITEM;
        PrivateChestCommon.OWNER_PLAYER_HEAD_ITEM = () -> OWNER_PLAYER_HEAD_ITEM;
        PrivateChestCommon.LOCKER_BLOCK_ENTITY_TYPE = () -> LOCKER_BLOCK_ENTITY_TYPE;
        PrivateChestCommon.LOCKER_MENU_TYPE = () -> LOCKER_MENU_TYPE;

        // Register Payloads
        PayloadTypeRegistry.serverboundPlay().register(PrivateChestCommonConfigPushPayload.TYPE, PrivateChestCommonConfigPushPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PrivateChestCommonConfigPushPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer serverPlayer = context.player();
                if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("privatechest.configuration.push_denied")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }

                Config.setEnableLockerCrafting(payload.enableLockerCrafting());

                MinecraftServer server = serverPlayer.level().getServer();
                if (server != null) {
                    server.reloadResources(server.getPackRepository().getSelectedIds())
                            .thenRun(() -> LOGGER.info("Reloaded datapacks after privatechest config push"));
                }

                LOGGER.info("Private Chest common config pushed by {}: enableLockerCrafting={}",
                        serverPlayer.getGameProfile().name(),
                        Config.isEnableLockerCrafting());
                serverPlayer.sendSystemMessage(
                        Component.translatable("privatechest.configuration.push_ok")
                                .withStyle(ChatFormatting.GREEN));
            });
        });

        // Block Break Protection
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }

            if (state.getBlock() instanceof PlayerHeadBlock) {
                BlockPos middlePos = pos.below();
                BlockState middleState = level.getBlockState(middlePos);
                if (middleState.is(LOCKER_BLOCK) && middleState.getValue(LockerBlock.PART) == LockerBlock.LockerPart.MIDDLE) {
                    BlockPos bottomPos = middlePos.below();
                    BlockEntity be = level.getBlockEntity(bottomPos);
                    if (be instanceof LockerBlockEntity lockerBe) {
                        if (!lockerBe.isOwner(player)) {
                            player.sendSystemMessage(
                                    Component.translatable("privatechest.message.no_break_permission").withStyle(ChatFormatting.RED));
                            return false;
                        }
                    }
                    LOCKER_BLOCK.breakEntireStructure(level, bottomPos, player, null);
                    return false;
                }
            }

            if (state.is(LOCKER_BLOCK)) {
                LockerBlock.LockerPart part = state.getValue(LockerBlock.PART);
                BlockPos bottomPos = part == LockerBlock.LockerPart.BOTTOM ? pos : pos.below();
                BlockEntity be = level.getBlockEntity(bottomPos);
                if (be instanceof LockerBlockEntity lockerBe) {
                    if (!lockerBe.isOwner(player)) {
                        player.sendSystemMessage(
                                Component.translatable("privatechest.message.no_break_permission").withStyle(ChatFormatting.RED));
                        return false;
                    }
                }
            }
            return true;
        });

        // Command Registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("privatechest")
                    .then(Commands.literal("give")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("owner", StringArgumentType.string())
                                .executes(context -> giveHead(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets"),
                                        StringArgumentType.getString(context, "owner")
                                ))
                            )
                        )
                    )
            );
        });
    }

    private static int giveHead(CommandSourceStack source, Collection<ServerPlayer> targets, String ownerName) {
        ResolvableProfile profile = resolveOwnerProfile(ownerName);

        for (ServerPlayer target : targets) {
            ItemStack stack = OWNER_PLAYER_HEAD_ITEM.getDefaultInstance().copy();
            stack.set(DataComponents.PROFILE, profile);

            boolean added = target.getInventory().add(stack);
            if (added) {
                target.inventoryMenu.broadcastChanges();
            } else {
                target.drop(stack, false);
            }
        }

        source.sendSuccess(
                () -> Component.translatable("privatechest.message.head_granted", ownerName).withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static ResolvableProfile resolveOwnerProfile(String ownerName) {
        try {
            UUID uuid = UUID.fromString(ownerName);
            return ResolvableProfile.createUnresolved(uuid);
        } catch (IllegalArgumentException e) {
            return ResolvableProfile.createUnresolved(ownerName);
        }
    }
}
