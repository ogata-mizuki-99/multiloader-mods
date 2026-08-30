package com.ogatamizuki.radialteleport.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class RadialTeleportKeyBindings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "main")
    );

    public static KeyMapping WAYPOINT_MODIFIER;

    private RadialTeleportKeyBindings() {
    }

    public static void register(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        WAYPOINT_MODIFIER = new KeyMapping(
                "key.radial_teleport.waypoint_modifier",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                CATEGORY
        );
        event.register(WAYPOINT_MODIFIER);
    }

    public static boolean isWaypointModifierDown() {
        return WAYPOINT_MODIFIER != null && WAYPOINT_MODIFIER.isDown();
    }
}
