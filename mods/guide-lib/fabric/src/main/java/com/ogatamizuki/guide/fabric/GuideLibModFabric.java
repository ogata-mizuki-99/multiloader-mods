package com.ogatamizuki.guide.fabric;

import com.ogatamizuki.guide.GuideAccess;
import com.ogatamizuki.guide.GuideItems;
import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.GuideLibDataComponents;
import com.ogatamizuki.guide.model.GuideTheme;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;

public class GuideLibModFabric implements ModInitializer {
    public static DataComponentType<GuideAccess> GUIDE_ACCESS;
    public static SoundEvent CODEX_OPEN;
    public static SoundEvent CODEX_PAGE;
    public static SoundEvent TABLET_OPEN;
    public static SoundEvent TABLET_BEEP;
    public static CreativeModeTab TAB;

    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.parse("minecraft:tools_and_utilities"));

    @Override
    public void onInitialize() {
        GuideLibCommon.LOGGER.info("Guide Lib (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        // Register Data Component
        GUIDE_ACCESS = FabricRegistryHelper.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                GuideLibCommon.id("guide_access"),
                DataComponentType.<GuideAccess>builder()
                        .persistent(GuideAccess.CODEC)
                        .networkSynchronized(ByteBufCodecs.fromCodec(GuideAccess.CODEC))
                        .build()
        );
        // Bind data component supplier for common module
        GuideLibDataComponents.GUIDE_ACCESS = () -> GUIDE_ACCESS;

        // Register Sounds
        CODEX_OPEN = registerSound("codex_open");
        CODEX_PAGE = registerSound("codex_page");
        TABLET_OPEN = registerSound("tablet_open");
        TABLET_BEEP = registerSound("tablet_beep");

        // Register Creative Tab
        ResourceKey<CreativeModeTab> guideTabKey = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                GuideLibCommon.id("guide_tab")
        );
        TAB = FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                guideTabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.guide_lib"))
                        .icon(() -> GuideItems.createCodex(GuideTheme.BOOK_ID))
                        .displayItems((parameters, output) -> {
                            output.accept(GuideItems.createCodex(GuideTheme.BOOK_ID));
                            output.accept(GuideItems.createCodexTablet());
                        })
                        .build()
        );

        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(guideTabKey).register(output -> {
            output.accept(GuideItems.createCodex(GuideTheme.BOOK_ID));
            output.accept(GuideItems.createCodexTablet());
        });

        // Register right click handler for guide items
        GuideItemUseHandlerFabric.register();

        // Trigger Fabric API's paginateTabs() to assign pages & tab positions
        net.minecraft.world.item.CreativeModeTabs.validate();
    }

    private static SoundEvent registerSound(String name) {
        return FabricRegistryHelper.register(
                BuiltInRegistries.SOUND_EVENT,
                GuideLibCommon.id(name),
                SoundEvent.createVariableRangeEvent(GuideLibCommon.id(name))
        );
    }
}
