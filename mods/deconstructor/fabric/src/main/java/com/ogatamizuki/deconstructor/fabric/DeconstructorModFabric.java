package com.ogatamizuki.deconstructor.fabric;

import com.ogatamizuki.deconstructor.DeconstructorBlock;
import com.ogatamizuki.deconstructor.DeconstructorBlockEntity;
import com.ogatamizuki.deconstructor.DeconstructorBlocks;
import com.ogatamizuki.deconstructor.DeconstructorCommon;
import com.ogatamizuki.deconstructor.DeconstructorMenu;
import com.ogatamizuki.deconstructor.EnchantmentManagerBlock;
import com.ogatamizuki.deconstructor.EnchantmentManagerBlockEntity;
import com.ogatamizuki.deconstructor.EnchantmentManagerMenu;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DeconstructorModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        DeconstructorCommon.LOGGER.info("Deconstructor Mod (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        ResourceKey<Block> deconstructorBlockKey = ResourceKey.create(Registries.BLOCK, DeconstructorCommon.id("deconstructor"));
        ResourceKey<Block> precisionBlockKey = ResourceKey.create(Registries.BLOCK, DeconstructorCommon.id("precision_deconstructor"));
        ResourceKey<Block> enchantManagerBlockKey = ResourceKey.create(Registries.BLOCK, DeconstructorCommon.id("enchant_manager"));

        ResourceKey<Item> deconstructorItemKey = ResourceKey.create(Registries.ITEM, DeconstructorCommon.id("deconstructor"));
        ResourceKey<Item> precisionItemKey = ResourceKey.create(Registries.ITEM, DeconstructorCommon.id("precision_deconstructor"));
        ResourceKey<Item> enchantManagerItemKey = ResourceKey.create(Registries.ITEM, DeconstructorCommon.id("enchant_manager"));

        // Blocks
        DeconstructorBlock deconstructor = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                deconstructorBlockKey,
                new DeconstructorBlock(
                        BlockBehaviour.Properties.of()
                                .setId(deconstructorBlockKey)
                                .mapColor(MapColor.METAL)
                                .strength(3.5F)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops()
                                .noOcclusion(),
                        1
                )
        );

        DeconstructorBlock precisionDeconstructor = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                precisionBlockKey,
                new DeconstructorBlock(
                        BlockBehaviour.Properties.of()
                                .setId(precisionBlockKey)
                                .mapColor(MapColor.METAL)
                                .strength(3.5F)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops()
                                .noOcclusion(),
                        3
                )
        );

        EnchantmentManagerBlock enchantManager = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                enchantManagerBlockKey,
                new EnchantmentManagerBlock(
                        BlockBehaviour.Properties.of()
                                .setId(enchantManagerBlockKey)
                                .mapColor(MapColor.METAL)
                                .strength(3.5F)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops()
                                .noOcclusion()
                                .lightLevel(state -> 12)
                )
        );

        // Items
        BlockItem deconstructorItem = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                deconstructorItemKey,
                new BlockItem(deconstructor, new Item.Properties().setId(deconstructorItemKey))
        );

        BlockItem precisionDeconstructorItem = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                precisionItemKey,
                new BlockItem(precisionDeconstructor, new Item.Properties().setId(precisionItemKey))
        );

        BlockItem enchantManagerItem = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                enchantManagerItemKey,
                new BlockItem(enchantManager, new Item.Properties().setId(enchantManagerItemKey))
        );

        // Block Entity Types
        BlockEntityType<DeconstructorBlockEntity> deconstructorBeType = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                DeconstructorCommon.id("deconstructor"),
                FabricRegistryHelper.createBlockEntityType(DeconstructorBlockEntity::new, deconstructor, precisionDeconstructor)
        );

        BlockEntityType<EnchantmentManagerBlockEntity> enchantManagerBeType = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                DeconstructorCommon.id("enchant_manager"),
                FabricRegistryHelper.createBlockEntityType(EnchantmentManagerBlockEntity::new, enchantManager)
        );

        // Menus
        MenuType<DeconstructorMenu> deconstructorMenuType = FabricRegistryHelper.register(
                BuiltInRegistries.MENU,
                DeconstructorCommon.id("deconstructor"),
                FabricRegistryHelper.createMenuType((containerId, inv) -> new DeconstructorMenu(containerId, inv, (DeconstructorBlockEntity) null), FeatureFlags.VANILLA_SET)
        );

        MenuType<EnchantmentManagerMenu> enchantManagerMenuType = FabricRegistryHelper.register(
                BuiltInRegistries.MENU,
                DeconstructorCommon.id("enchant_manager"),
                FabricRegistryHelper.createMenuType((containerId, inv) -> new EnchantmentManagerMenu(containerId, inv, (EnchantmentManagerBlockEntity) null), FeatureFlags.VANILLA_SET)
        );

        // Creative Tab
        ResourceKey<CreativeModeTab> deconstructorTabKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                DeconstructorCommon.id("deconstructor_tab")
        );
        FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                deconstructorTabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.deconstructor"))
                        .icon(deconstructorItem::getDefaultInstance)
                        .displayItems((parameters, output) -> {
                            output.accept(deconstructorItem);
                            output.accept(precisionDeconstructorItem);
                            output.accept(enchantManagerItem);
                        }).build()
        );

        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(deconstructorTabKey).register(output -> {
            output.accept(deconstructorItem);
            output.accept(precisionDeconstructorItem);
            output.accept(enchantManagerItem);
        });

        // Bind holders to common references
        DeconstructorBlocks.DECONSTRUCTOR = () -> deconstructor;
        DeconstructorBlocks.PRECISION_DECONSTRUCTOR = () -> precisionDeconstructor;
        DeconstructorBlocks.ENCHANT_MANAGER = () -> enchantManager;
        DeconstructorBlocks.DECONSTRUCTOR_ITEM = () -> deconstructorItem;
        DeconstructorBlocks.PRECISION_DECONSTRUCTOR_ITEM = () -> precisionDeconstructorItem;
        DeconstructorBlocks.ENCHANT_MANAGER_ITEM = () -> enchantManagerItem;
        DeconstructorBlocks.DECONSTRUCTOR_BLOCK_ENTITY_TYPE = () -> deconstructorBeType;
        DeconstructorBlocks.ENCHANT_MANAGER_BLOCK_ENTITY_TYPE = () -> enchantManagerBeType;
        DeconstructorBlocks.DECONSTRUCTOR_MENU_TYPE = () -> deconstructorMenuType;
        DeconstructorBlocks.ENCHANT_MANAGER_MENU_TYPE = () -> enchantManagerMenuType;

        // Trigger Fabric API's paginateTabs() to assign pages & tab positions
        net.minecraft.world.item.CreativeModeTabs.validate();
    }
}
