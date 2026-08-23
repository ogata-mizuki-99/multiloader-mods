package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.NetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.UUID;

public final class ShadowAppearanceEffects {
    private static int particleTick;

    private ShadowAppearanceEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (LookalikeClientShadows.shadowPlayers().isEmpty() && LookalikeClientShadows.paths().isEmpty()) {
            particleTick = 0;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        particleTick++;
        if (particleTick % 4 != 0) {
            return;
        }

        RandomSource random = minecraft.player.level().getRandom();
        for (UUID shadowUuid : LookalikeClientShadows.shadowPlayers()) {
            Player shadowPlayer = minecraft.level.getPlayerByUUID(shadowUuid);
            if (shadowPlayer != null) {
                spawnShadowParticles(minecraft, shadowPlayer.position().add(0, 1.0, 0), random);
            }
        }

        if (LookalikeClientShadows.isPathVisualizationEnabled()) {
            for (NetworkPayloads.ShadowPathEntry path : LookalikeClientShadows.paths()) {
                double progress = (particleTick % 40) / 40.0;
                Vec3 start = Vec3.atCenterOf(new BlockPos(path.fromX(), path.fromY(), path.fromZ())).add(0, 0.5, 0);
                Vec3 end = Vec3.atCenterOf(new BlockPos(path.toX(), path.toY(), path.toZ())).add(0, 0.5, 0);
                spawnShadowParticles(minecraft, start.lerp(end, progress), random);
            }
        }
    }

    private static void spawnShadowParticles(Minecraft minecraft, Vec3 center, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.6;
            double oy = random.nextDouble() * 1.2;
            double oz = (random.nextDouble() - 0.5) * 0.6;
            minecraft.level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    center.x + ox,
                    center.y + oy,
                    center.z + oz,
                    0.0,
                    0.02,
                    0.0
            );
        }
        minecraft.level.addParticle(
                ParticleTypes.SMOKE,
                center.x,
                center.y + 0.8,
                center.z,
                0.0,
                0.01,
                0.0
        );
    }
}
