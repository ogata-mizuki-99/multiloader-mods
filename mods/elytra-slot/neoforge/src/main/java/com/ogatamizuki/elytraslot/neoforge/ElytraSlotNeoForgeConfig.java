package com.ogatamizuki.elytraslot.neoforge;

import com.ogatamizuki.elytraslot.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ElytraSlotNeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ELYTRA_SLOT_X;
    public static final ModConfigSpec.IntValue ELYTRA_SLOT_Y;
    public static final ModConfigSpec.IntValue FIREWORK_SLOT_X;
    public static final ModConfigSpec.IntValue FIREWORK_SLOT_Y;
    public static final ModConfigSpec.IntValue CREATIVE_ELYTRA_SLOT_X;
    public static final ModConfigSpec.IntValue CREATIVE_ELYTRA_SLOT_Y;
    public static final ModConfigSpec.IntValue CREATIVE_FIREWORK_SLOT_X;
    public static final ModConfigSpec.IntValue CREATIVE_FIREWORK_SLOT_Y;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.IntValue ELYTRA_HUD_X;
    public static final ModConfigSpec.IntValue ELYTRA_HUD_Y;
    public static final ModConfigSpec.IntValue FIREWORK_HUD_X;
    public static final ModConfigSpec.IntValue FIREWORK_HUD_Y;
    public static final ModConfigSpec.DoubleValue WARNING_THRESHOLD;

    static {
        BUILDER.push("slots");
        ELYTRA_SLOT_X = BUILDER.comment("Inventory elytra slot X offset from inventory panel left (Default: 77, negative = outside panel)")
                .defineInRange("elytra_slot_x", 77, -300, 300);
        ELYTRA_SLOT_Y = BUILDER.comment("Inventory elytra slot Y offset from inventory panel top (Default: 26, negative = outside panel)")
                .defineInRange("elytra_slot_y", 26, -300, 300);
        FIREWORK_SLOT_X = BUILDER.comment("Inventory firework slot X offset from inventory panel left (Default: 77, negative = outside panel)")
                .defineInRange("firework_slot_x", 77, -300, 300);
        FIREWORK_SLOT_Y = BUILDER.comment("Inventory firework slot Y offset from inventory panel top (Default: 8, negative = outside panel)")
                .defineInRange("firework_slot_y", 8, -300, 300);
        CREATIVE_ELYTRA_SLOT_X = BUILDER.comment("Creative survival-inventory tab elytra slot X (Default: 126)")
                .defineInRange("creative_elytra_slot_x", 126, -300, 300);
        CREATIVE_ELYTRA_SLOT_Y = BUILDER.comment("Creative survival-inventory tab elytra slot Y (Default: 33, aligned with armor slot spacing)")
                .defineInRange("creative_elytra_slot_y", 33, -300, 300);
        CREATIVE_FIREWORK_SLOT_X = BUILDER.comment("Creative survival-inventory tab firework slot X (Default: 126)")
                .defineInRange("creative_firework_slot_x", 126, -300, 300);
        CREATIVE_FIREWORK_SLOT_Y = BUILDER.comment("Creative survival-inventory tab firework slot Y (Default: 6, aligned with armor slot spacing)")
                .defineInRange("creative_firework_slot_y", 6, -300, 300);
        BUILDER.pop();

        BUILDER.push("hud");
        HUD_ENABLED = BUILDER.comment("Enable Elytra Durability & Firework HUD (Default: true)")
                .define("hud_enabled", true);
        ELYTRA_HUD_X = BUILDER.comment("Elytra HUD X offset from screen bottom center (Default: -150, left of offhand)")
                .defineInRange("elytra_hud_x", -150, -1000, 1000);
        ELYTRA_HUD_Y = BUILDER.comment("Elytra HUD Y offset from screen bottom (Default: -22)")
                .defineInRange("elytra_hud_y", -22, -1000, 1000);
        FIREWORK_HUD_X = BUILDER.comment("Firework HUD X offset from screen bottom center (Default: -170, left of offhand)")
                .defineInRange("firework_hud_x", -170, -1000, 1000);
        FIREWORK_HUD_Y = BUILDER.comment("Firework HUD Y offset from screen bottom (Default: -22)")
                .defineInRange("firework_hud_y", -22, -1000, 1000);
        WARNING_THRESHOLD = BUILDER.comment("Durability threshold fraction to show a warning (Default: 0.05 for 5%)")
                .defineInRange("warning_threshold", 0.05, 0.0, 1.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void init() {
        Config.ELYTRA_SLOT_X.bind(ELYTRA_SLOT_X::get, ELYTRA_SLOT_X::set, ELYTRA_SLOT_X::save);
        Config.ELYTRA_SLOT_Y.bind(ELYTRA_SLOT_Y::get, ELYTRA_SLOT_Y::set, ELYTRA_SLOT_Y::save);
        Config.FIREWORK_SLOT_X.bind(FIREWORK_SLOT_X::get, FIREWORK_SLOT_X::set, FIREWORK_SLOT_X::save);
        Config.FIREWORK_SLOT_Y.bind(FIREWORK_SLOT_Y::get, FIREWORK_SLOT_Y::set, FIREWORK_SLOT_Y::save);
        Config.CREATIVE_ELYTRA_SLOT_X.bind(CREATIVE_ELYTRA_SLOT_X::get, CREATIVE_ELYTRA_SLOT_X::set, CREATIVE_ELYTRA_SLOT_X::save);
        Config.CREATIVE_ELYTRA_SLOT_Y.bind(CREATIVE_ELYTRA_SLOT_Y::get, CREATIVE_ELYTRA_SLOT_Y::set, CREATIVE_ELYTRA_SLOT_Y::save);
        Config.CREATIVE_FIREWORK_SLOT_X.bind(CREATIVE_FIREWORK_SLOT_X::get, CREATIVE_FIREWORK_SLOT_X::set, CREATIVE_FIREWORK_SLOT_X::save);
        Config.CREATIVE_FIREWORK_SLOT_Y.bind(CREATIVE_FIREWORK_SLOT_Y::get, CREATIVE_FIREWORK_SLOT_Y::set, CREATIVE_FIREWORK_SLOT_Y::save);

        Config.HUD_ENABLED.bind(HUD_ENABLED::get, HUD_ENABLED::set, HUD_ENABLED::save);
        Config.ELYTRA_HUD_X.bind(ELYTRA_HUD_X::get, ELYTRA_HUD_X::set, ELYTRA_HUD_X::save);
        Config.ELYTRA_HUD_Y.bind(ELYTRA_HUD_Y::get, ELYTRA_HUD_Y::set, ELYTRA_HUD_Y::save);
        Config.FIREWORK_HUD_X.bind(FIREWORK_HUD_X::get, FIREWORK_HUD_X::set, FIREWORK_HUD_X::save);
        Config.FIREWORK_HUD_Y.bind(FIREWORK_HUD_Y::get, FIREWORK_HUD_Y::set, FIREWORK_HUD_Y::save);
        Config.WARNING_THRESHOLD.bind(WARNING_THRESHOLD::get, WARNING_THRESHOLD::set, WARNING_THRESHOLD::save);
    }
}
