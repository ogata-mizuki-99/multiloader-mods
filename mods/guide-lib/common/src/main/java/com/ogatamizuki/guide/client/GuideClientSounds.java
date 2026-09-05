package com.ogatamizuki.guide.client;

import com.ogatamizuki.guide.GuideSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public final class GuideClientSounds {
    private GuideClientSounds() {}

    public static void play(Player player, Identifier soundId, float volume, float pitch) {
        SoundEvent soundEvent = GuideSounds.resolve(soundId);
        if (soundEvent == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (player != null && player.level().isClientSide()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch));
            return;
        }
        if (player != null) {
            player.playSound(soundEvent, volume, pitch);
        }
    }
}
