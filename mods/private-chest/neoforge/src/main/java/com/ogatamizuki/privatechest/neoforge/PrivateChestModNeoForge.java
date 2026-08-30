package com.ogatamizuki.privatechest.neoforge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.MapCodec;
import com.ogatamizuki.privatechest.*;
import com.ogatamizuki.privatechest.neoforge.CraftingRecipeEnabledCondition;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod(PrivateChestModNeoForge.MODID)
public class PrivateChestModNeoForge {
    public static final String MODID = "privatechest";
    public static final Logger LOGGER = LogManager.getLogger(PrivateChestModNeoForge.class);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final DeferredBlock<LockerBlock> LOCKER_BLOCK = BLOCKS.registerBlock("locker",
            LockerBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(2.5F, 1200.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

    public static final DeferredItem<BlockItem> LOCKER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("locker", LOCKER_BLOCK);

    /** バニラ PLAYER_HEAD を設置・装備するアイテム（SkullBlockEntity 互換のため専用ブロックは持たない） */
    public static final DeferredItem<OwnerPlayerHeadItem> OWNER_PLAYER_HEAD_ITEM = ITEMS.registerItem("owner_player_head",
            p -> new OwnerPlayerHeadItem(p.stacksTo(64).equippableUnswappable(EquipmentSlot.HEAD))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockerBlockEntity>> LOCKER_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("locker",
            () -> new BlockEntityType<>(LockerBlockEntity::new, java.util.Set.of(LOCKER_BLOCK.get()))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<LockerMenu>> LOCKER_MENU_TYPE = MENUS.register("locker",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                if (data != null) {
                    return new LockerMenu(windowId, inv, data.readBlockPos());
                }
                // クライアントは open_screen 時に extraData が null。同期はコンテナパケットで行われる。
                return new LockerMenu(windowId, inv, (LockerBlockEntity) null);
            })
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("private_chest_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.privatechest"))
                    .icon(() -> LOCKER_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(LOCKER_BLOCK_ITEM.get());
                        output.accept(OWNER_PLAYER_HEAD_ITEM.get().getDefaultInstance());
                    }).build()
    );

    public PrivateChestModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Private Locker Chest Mod (NeoForge) Initializing...");

        PrivateChestCommon.LOCKER_BLOCK = LOCKER_BLOCK;
        PrivateChestCommon.LOCKER_BLOCK_ITEM = LOCKER_BLOCK_ITEM;
        PrivateChestCommon.OWNER_PLAYER_HEAD_ITEM = OWNER_PLAYER_HEAD_ITEM;
        PrivateChestCommon.LOCKER_BLOCK_ENTITY_TYPE = LOCKER_BLOCK_ENTITY_TYPE;
        PrivateChestCommon.LOCKER_MENU_TYPE = LOCKER_MENU_TYPE;

        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeoForge.SPEC);
        modEventBus.addListener((ModConfigEvent.Loading e) -> ConfigNeoForge.sync());

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CONDITION_CODECS.register(modEventBus);

        CONDITION_CODECS.register("crafting_recipe_enabled", () -> CraftingRecipeEnabledCondition.CODEC);

        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::registerPayloads);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                PrivateChestCommonConfigPushPayload.TYPE,
                PrivateChestCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush);
    }

    private void handleCommonConfigPush(PrivateChestCommonConfigPushPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("privatechest.configuration.push_denied")
                                .withStyle(ChatFormatting.RED));
                return;
            }

            ConfigNeoForge.updateFromPush(payload.enableLockerCrafting());

            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds())
                        .thenRun(() -> LOGGER.info("Reloaded datapacks after privatechest config push")));
            }

            LOGGER.info(
                    "Private Chest common config pushed by {}: enableLockerCrafting={}",
                    serverPlayer.getGameProfile().name(),
                    Config.isEnableLockerCrafting());
            serverPlayer.sendSystemMessage(
                    Component.translatable("privatechest.configuration.push_ok")
                            .withStyle(ChatFormatting.GREEN));
        });
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ConfigNeoForge.SPEC) {
            return;
        }
        ConfigNeoForge.sync();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> LOGGER.info("Reloaded datapacks after privatechest config change")));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Private Chest commands");
        event.getDispatcher().register(
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
    }

    private int giveHead(CommandSourceStack source, java.util.Collection<ServerPlayer> targets, String ownerName) {
        ResolvableProfile profile = resolveOwnerProfile(ownerName);

        for (ServerPlayer target : targets) {
            ItemStack stack = OWNER_PLAYER_HEAD_ITEM.get().getDefaultInstance().copy();
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

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> LockerStructureUtil.isLockerStructureBlock(event.getLevel(), pos));
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Player player = event.getPlayer();

        if (state.getBlock() instanceof PlayerHeadBlock) {
            BlockPos middlePos = pos.below();
            BlockState middleState = level.getBlockState(middlePos);
            if (middleState.is(LOCKER_BLOCK.get()) && middleState.getValue(LockerBlock.PART) == LockerBlock.LockerPart.MIDDLE) {
                BlockPos bottomPos = middlePos.below();
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(bottomPos);
                if (be instanceof LockerBlockEntity lockerBe) {
                    if (!lockerBe.isOwner(player)) {
                        event.setCanceled(true);
                        player.sendSystemMessage(
                                Component.translatable("privatechest.message.no_break_permission").withStyle(ChatFormatting.RED));
                        return;
                    }
                }
                event.setCanceled(true);
                LOCKER_BLOCK.get().breakEntireStructure(level, bottomPos, player, null);
            }
        }

        if (state.is(LOCKER_BLOCK.get())) {
            LockerBlock.LockerPart part = state.getValue(LockerBlock.PART);
            BlockPos bottomPos = part == LockerBlock.LockerPart.BOTTOM ? pos : pos.below();
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(bottomPos);
            if (be instanceof LockerBlockEntity lockerBe) {
                if (!lockerBe.isOwner(player)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(
                            Component.translatable("privatechest.message.no_break_permission").withStyle(ChatFormatting.RED));
                }
            }
        }
    }
}
