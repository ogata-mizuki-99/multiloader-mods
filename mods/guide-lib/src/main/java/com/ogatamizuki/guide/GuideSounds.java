package com.ogatamizuki.guide;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.Map;
import java.util.function.Supplier;

public final class GuideSounds {
    private static final Map<Identifier, Supplier<SoundEvent>> REGISTERED = Map.of(
            GuideLibMod.CODEX_OPEN.getId(), GuideLibMod.CODEX_OPEN,
            GuideLibMod.CODEX_PAGE.getId(), GuideLibMod.CODEX_PAGE,
            GuideLibMod.TABLET_OPEN.getId(), GuideLibMod.TABLET_OPEN,
            GuideLibMod.TABLET_BEEP.getId(), GuideLibMod.TABLET_BEEP
    );

    private GuideSounds() {}

    public static SoundEvent resolve(Identifier soundId) {
        if (soundId == null) {
            return null;
        }
        Supplier<SoundEvent> registered = REGISTERED.get(soundId);
        if (registered != null) {
            return registered.get();
        }
        return BuiltInRegistries.SOUND_EVENT.get(soundId).map(Holder::value).orElse(null);
    }
}
