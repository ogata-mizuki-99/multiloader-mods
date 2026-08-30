package com.ogatamizuki.lookalike.cast;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;

public enum CastEffectTemplate {
    WITCH_SMOKE(ParticleTypes.WITCH, SoundEvents.WITCH_AMBIENT, 0.4F, 1.2F),
    ENDER(ParticleTypes.REVERSE_PORTAL, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.0F),
    PORTAL(ParticleTypes.PORTAL, SoundEvents.PORTAL_TRIGGER, 0.5F, 1.0F),
    NONE(null, null, 0.0F, 1.0F);

    private final ParticleOptions particle;
    private final SoundEvent sound;
    private final float soundVolume;
    private final float soundPitch;

    CastEffectTemplate(ParticleOptions particle, SoundEvent sound, float soundVolume, float soundPitch) {
        this.particle = particle;
        this.sound = sound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    public static CastEffectTemplate fromName(String name) {
        if (name == null || name.isBlank()) {
            return WITCH_SMOKE;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return WITCH_SMOKE;
        }
    }

    public void play(ServerPlayer player, boolean playSound) {
        if (this == NONE) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(
                particle,
                player.getX(),
                player.getY() + 0.15,
                player.getZ(),
                12,
                0.35,
                0.25,
                0.35,
                0.02
        );
        if (playSound && sound != null) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    sound,
                    SoundSource.PLAYERS,
                    soundVolume,
                    soundPitch
            );
        }
    }
}
