package com.ogatamizuki.deconstructor.neoforge;

import com.ogatamizuki.deconstructor.DeconstructorBlock;
import com.ogatamizuki.deconstructor.DeconstructorBlockEntity;
import com.ogatamizuki.deconstructor.DeconstructorBlocks;
import com.ogatamizuki.deconstructor.DeconstructorCommon;
import com.ogatamizuki.deconstructor.DeconstructorCommonConfigPushPayload;
import com.ogatamizuki.deconstructor.DeconstructorMenu;
import com.ogatamizuki.deconstructor.DeconstructorRecipeIndex;
import com.ogatamizuki.deconstructor.DeconstructorScreen;
import com.ogatamizuki.deconstructor.EnchantmentManagerBlock;
import com.ogatamizuki.deconstructor.EnchantmentManagerBlockEntity;
import com.ogatamizuki.deconstructor.EnchantmentManagerMenu;
import com.ogatamizuki.deconstructor.EnchantmentManagerRenderer;
import com.ogatamizuki.deconstructor.EnchantmentManagerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DeconstructorCommon.MODID)
public class DeconstructorModNeoForge {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DeconstructorCommon.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeconstructorCommon.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeconstructorCommon.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DeconstructorCommon.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeconstructorCommon.MODID);

    // 解体機 (1つ選択回収)
    public static final DeferredBlock<DeconstructorBlock> DECONSTRUCTOR = BLOCKS.registerBlock("deconstructor",
            p -> new DeconstructorBlock(p, 1),
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
    );

    // 精密解体機 (3つ選択回収)
    public static final DeferredBlock<DeconstructorBlock> PRECISION_DECONSTRUCTOR = BLOCKS.registerBlock("precision_deconstructor",
            p -> new DeconstructorBlock(p, 3),
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
    );

    // エンチャント管理機 (enchant_manager)
    public static final DeferredBlock<EnchantmentManagerBlock> ENCHANT_MANAGER = BLOCKS.registerBlock("enchant_manager",
            EnchantmentManagerBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 12)
    );

    public static final DeferredItem<BlockItem> DECONSTRUCTOR_ITEM = ITEMS.registerSimpleBlockItem("deconstructor", DECONSTRUCTOR);
    public static final DeferredItem<BlockItem> PRECISION_DECONSTRUCTOR_ITEM = ITEMS.registerSimpleBlockItem("precision_deconstructor", PRECISION_DECONSTRUCTOR);
    public static final DeferredItem<BlockItem> ENCHANT_MANAGER_ITEM = ITEMS.registerSimpleBlockItem("enchant_manager", ENCHANT_MANAGER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeconstructorBlockEntity>> DECONSTRUCTOR_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("deconstructor",
            () -> new BlockEntityType<>(DeconstructorBlockEntity::new, java.util.Set.of(DECONSTRUCTOR.get(), PRECISION_DECONSTRUCTOR.get()))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantmentManagerBlockEntity>> ENCHANT_MANAGER_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("enchant_manager",
            () -> new BlockEntityType<>(EnchantmentManagerBlockEntity::new, java.util.Set.of(ENCHANT_MANAGER.get()))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<DeconstructorMenu>> DECONSTRUCTOR_MENU_TYPE = MENUS.register("deconstructor",
            () -> new MenuType<>((windowId, inv) -> new DeconstructorMenu(windowId, inv, (DeconstructorBlockEntity) null), net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantmentManagerMenu>> ENCHANT_MANAGER_MENU_TYPE = MENUS.register("enchant_manager",
            () -> new MenuType<>((windowId, inv) -> new EnchantmentManagerMenu(windowId, inv, (EnchantmentManagerBlockEntity) null), net.minecraft.world.flag.FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("deconstructor_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deconstructor"))
                    .icon(() -> DECONSTRUCTOR_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(DECONSTRUCTOR_ITEM.get());
                        output.accept(PRECISION_DECONSTRUCTOR_ITEM.get());
                        output.accept(ENCHANT_MANAGER_ITEM.get());
                    }).build()
    );

    public DeconstructorModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        DeconstructorCommon.LOGGER.info("Deconstructor Mod (NeoForge) Initializing...");

        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeoForge.SPEC);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::registerPayloads);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Bind holders to common references
        DeconstructorBlocks.DECONSTRUCTOR = DECONSTRUCTOR;
        DeconstructorBlocks.PRECISION_DECONSTRUCTOR = PRECISION_DECONSTRUCTOR;
        DeconstructorBlocks.ENCHANT_MANAGER = ENCHANT_MANAGER;
        DeconstructorBlocks.DECONSTRUCTOR_ITEM = DECONSTRUCTOR_ITEM;
        DeconstructorBlocks.PRECISION_DECONSTRUCTOR_ITEM = PRECISION_DECONSTRUCTOR_ITEM;
        DeconstructorBlocks.ENCHANT_MANAGER_ITEM = ENCHANT_MANAGER_ITEM;
        DeconstructorBlocks.DECONSTRUCTOR_BLOCK_ENTITY_TYPE = DECONSTRUCTOR_BLOCK_ENTITY_TYPE;
        DeconstructorBlocks.ENCHANT_MANAGER_BLOCK_ENTITY_TYPE = ENCHANT_MANAGER_BLOCK_ENTITY_TYPE;
        DeconstructorBlocks.DECONSTRUCTOR_MENU_TYPE = DECONSTRUCTOR_MENU_TYPE;
        DeconstructorBlocks.ENCHANT_MANAGER_MENU_TYPE = ENCHANT_MANAGER_MENU_TYPE;

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
            modEventBus.addListener(this::registerRenderers);
        }
    }

    private void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ConfigNeoForge.SPEC) {
            ConfigNeoForge.sync();
            DeconstructorRecipeIndex.invalidate();
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                DeconstructorCommonConfigPushPayload.TYPE,
                DeconstructorCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush);
    }

    private void handleCommonConfigPush(DeconstructorCommonConfigPushPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("deconstructor.configuration.push_denied")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }

            String excludedItems = payload.excludedItems() == null ? "" : payload.excludedItems();
            ConfigNeoForge.EXCLUDED_ITEMS.set(excludedItems);
            ConfigNeoForge.EXCLUDED_ITEMS.save();
            ConfigNeoForge.sync();
            DeconstructorRecipeIndex.invalidate();

            DeconstructorCommon.LOGGER.info(
                    "Deconstructor common config pushed by {}: excludedItems={}",
                    serverPlayer.getGameProfile().name(),
                    ConfigNeoForge.EXCLUDED_ITEMS.get());
            serverPlayer.sendSystemMessage(
                    Component.translatable("deconstructor.configuration.push_ok")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
        });
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENCHANT_MANAGER_BLOCK_ENTITY_TYPE.get(), EnchantmentManagerRenderer::new);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DECONSTRUCTOR_MENU_TYPE.get(), DeconstructorScreen::new);
        event.register(ENCHANT_MANAGER_MENU_TYPE.get(), EnchantmentManagerScreen::new);
    }
}
