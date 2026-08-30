package com.ogatamizuki.radialteleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record PlayerWaypoint(
        UUID waypointId,
        String name,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public static final Codec<PlayerWaypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(PlayerWaypoint::waypointId),
            Codec.STRING.fieldOf("name").forGetter(PlayerWaypoint::name),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(PlayerWaypoint::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(PlayerWaypoint::x),
            Codec.DOUBLE.fieldOf("y").forGetter(PlayerWaypoint::y),
            Codec.DOUBLE.fieldOf("z").forGetter(PlayerWaypoint::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(PlayerWaypoint::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(PlayerWaypoint::pitch)
    ).apply(instance, PlayerWaypoint::new));

    public String destinationId() {
        return TeleportService.WAYPOINT_DESTINATION_PREFIX + waypointId;
    }

    public static PlayerWaypoint fromPlayer(String name, net.minecraft.server.level.ServerPlayer player) {
        return new PlayerWaypoint(
                UUID.randomUUID(),
                name,
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    public static ResourceKey<Level> parseDimension(String id) {
        Identifier location = Identifier.parse(id);
        return ResourceKey.create(Registries.DIMENSION, location);
    }
}
