package com.ogatamizuki.deconstructor;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DeconstructorMod.MODID)
public class DeconstructorMod {
    public static final String MODID = "deconstructor";
    public static final Logger LOGGER = LogManager.getLogger(DeconstructorMod.class);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

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
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new DeconstructorMenu(windowId, inv, data.readBlockPos()))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantmentManagerMenu>> ENCHANT_MANAGER_MENU_TYPE = MENUS.register("enchant_manager",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new EnchantmentManagerMenu(windowId, inv, data.readBlockPos()))
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

    public DeconstructorMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Deconstructor Mod Initializing...");

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::registerPayloads);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(this::registerScreens);
            modEventBus.addListener(this::registerRenderers);
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
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
            Config.EXCLUDED_ITEMS.set(excludedItems);
            Config.EXCLUDED_ITEMS.save();
            DeconstructorRecipeIndex.invalidate();

            LOGGER.info(
                    "Deconstructor common config pushed by {}: excludedItems={}",
                    serverPlayer.getGameProfile().name(),
                    Config.EXCLUDED_ITEMS.get());
            serverPlayer.sendSystemMessage(
                    Component.translatable("deconstructor.configuration.push_ok")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
        });
    }

    private void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ENCHANT_MANAGER_BLOCK_ENTITY_TYPE.get(), EnchantmentManagerRenderer::new);
    }


    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DECONSTRUCTOR_MENU_TYPE.get(), DeconstructorScreen::new);
        event.register(ENCHANT_MANAGER_MENU_TYPE.get(), EnchantmentManagerScreen::new);
    }
}
