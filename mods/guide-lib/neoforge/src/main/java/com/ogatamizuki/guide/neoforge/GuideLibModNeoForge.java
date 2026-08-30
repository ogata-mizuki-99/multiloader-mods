package com.ogatamizuki.guide.neoforge;

import com.ogatamizuki.guide.GuideBookLoader;
import com.ogatamizuki.guide.GuideItems;
import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.GuideLibDataComponents;
import com.ogatamizuki.guide.GuideManualLoader;
import com.ogatamizuki.guide.GuideThemeLoader;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.GuideAccess;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GuideLibCommon.MODID)
public class GuideLibModNeoForge {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GuideLibCommon.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, GuideLibCommon.MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GuideLibCommon.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GuideAccess>> GUIDE_ACCESS =
            DATA_COMPONENTS.registerComponentType(
                    "guide_access",
                    builder -> builder
                            .persistent(GuideAccess.CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodec(GuideAccess.CODEC))
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> CODEX_OPEN = registerSound("codex_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CODEX_PAGE = registerSound("codex_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLET_OPEN = registerSound("tablet_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> TABLET_BEEP = registerSound("tablet_beep");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "guide_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guide_lib"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .icon(() -> GuideItems.createCodex(GuideTheme.BOOK_ID))
                    .displayItems((parameters, output) -> {
                        output.accept(GuideItems.createCodex(GuideTheme.BOOK_ID));
                        output.accept(GuideItems.createCodexTablet());
                    })
                    .build()
    );

    public GuideLibModNeoForge(IEventBus modEventBus) {
        GuideLibCommon.LOGGER.info("Guide Lib (NeoForge) Initializing...");

        DATA_COMPONENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        // Bind data component supplier for common module
        GuideLibDataComponents.GUIDE_ACCESS = GUIDE_ACCESS;

        NeoForge.EVENT_BUS.addListener(this::onAddServerReloadListeners);
        NeoForge.EVENT_BUS.register(GuideItemUseHandlerNeoForge.class);
        NeoForge.EVENT_BUS.register(GuideLegacyItemMigratorNeoForge.class);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            GuideLibClientNeoForge.init(modEventBus);
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(GuideLibCommon.id(name)));
    }

    private void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(GuideBookLoader.LISTENER_ID, new GuideBookLoader());
        event.addListener(GuideThemeLoader.LISTENER_ID, new GuideThemeLoader());
        event.addListener(GuideManualLoader.LISTENER_ID, new GuideManualLoader());
    }

    public static void openBook(Identifier bookId) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            GuideLibClient.openBook(null, bookId);
        }
    }
}
