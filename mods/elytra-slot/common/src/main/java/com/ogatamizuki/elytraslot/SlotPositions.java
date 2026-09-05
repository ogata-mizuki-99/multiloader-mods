package com.ogatamizuki.elytraslot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SlotPositions(
        int elytraX,
        int elytraY,
        int fireworkX,
        int fireworkY,
        int creativeElytraX,
        int creativeElytraY,
        int creativeFireworkX,
        int creativeFireworkY
) {
    public static final SlotPositions DEFAULT = new SlotPositions(77, 26, 77, 8, 126, 33, 126, 6);

    private static final Codec<SlotPositions> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("elytra_x").forGetter(SlotPositions::elytraX),
            Codec.INT.fieldOf("elytra_y").forGetter(SlotPositions::elytraY),
            Codec.INT.fieldOf("firework_x").forGetter(SlotPositions::fireworkX),
            Codec.INT.fieldOf("firework_y").forGetter(SlotPositions::fireworkY),
            Codec.INT.optionalFieldOf("creative_elytra_x", DEFAULT.creativeElytraX()).forGetter(SlotPositions::creativeElytraX),
            Codec.INT.optionalFieldOf("creative_elytra_y", DEFAULT.creativeElytraY()).forGetter(SlotPositions::creativeElytraY),
            Codec.INT.optionalFieldOf("creative_firework_x", DEFAULT.creativeFireworkX()).forGetter(SlotPositions::creativeFireworkX),
            Codec.INT.optionalFieldOf("creative_firework_y", DEFAULT.creativeFireworkY()).forGetter(SlotPositions::creativeFireworkY)
    ).apply(instance, SlotPositions::new));

    private static final Codec<SlotPositions> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("elytra_x").forGetter(SlotPositions::elytraX),
            Codec.INT.fieldOf("elytra_y").forGetter(SlotPositions::elytraY),
            Codec.INT.fieldOf("firework_x").forGetter(SlotPositions::fireworkX),
            Codec.INT.fieldOf("firework_y").forGetter(SlotPositions::fireworkY)
    ).apply(instance, (elytraX, elytraY, fireworkX, fireworkY) -> new SlotPositions(
            elytraX, elytraY, fireworkX, fireworkY,
            DEFAULT.creativeElytraX(), DEFAULT.creativeElytraY(),
            DEFAULT.creativeFireworkX(), DEFAULT.creativeFireworkY()
    )));

    public static final Codec<SlotPositions> CODEC = Codec.withAlternative(FULL_CODEC, LEGACY_CODEC);

    public int[] forSurvival() {
        return new int[]{elytraX, elytraY, fireworkX, fireworkY};
    }

    public int[] forCreative() {
        return new int[]{creativeElytraX, creativeElytraY, creativeFireworkX, creativeFireworkY};
    }

    public int[] resolve(boolean creative) {
        return creative ? forCreative() : forSurvival();
    }
}
