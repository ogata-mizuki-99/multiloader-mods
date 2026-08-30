package com.ogatamizuki.guide;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class GuideSounds {
    public static final Identifier CODEX_OPEN_ID = GuideLibCommon.id("codex_open");
    public static final Identifier CODEX_PAGE_ID = GuideLibCommon.id("codex_page");
    public static final Identifier TABLET_OPEN_ID = GuideLibCommon.id("tablet_open");
    public static final Identifier TABLET_BEEP_ID = GuideLibCommon.id("tablet_beep");

    private GuideSounds() {}

    public static SoundEvent resolve(Identifier soundId) {
        if (soundId == null) {
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.get(soundId).map(Holder::value).orElse(null);
    }
}
