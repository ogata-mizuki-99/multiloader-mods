package com.ogatamizuki.guide.client;

import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.GuideSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public final class GuideClientSounds {
    private GuideClientSounds() {}

    public static void play(Player player, Identifier soundId, float volume, float pitch) {
        SoundEvent soundEvent = GuideSounds.resolve(soundId);
        if (soundEvent == null) {
            if (soundId != null) {
                GuideLibCommon.LOGGER.warn("Guide sound is not registered: {}", soundId);
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (player != null && player.level().isClientSide()) {
            // forUI(event, pitch) hardcodes volume 0.25 and drops the requested volume.
            SoundEngine.PlayResult result = minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(soundEvent, pitch, volume));
            if (result != SoundEngine.PlayResult.NOT_STARTED) {
                return;
            }
        }
        if (player != null) {
            player.playSound(soundEvent, volume, pitch);
        }
    }
}
