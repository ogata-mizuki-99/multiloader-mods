package com.ogatamizuki.economy.backend.local;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLocalPlayerStatsResetTest {

    @Test
    void applyStatsReset_zerosPlayTimeWhenEnabled() {
        JsonObject root = JsonParser.parseString("""
                {
                  "stats": {
                    "minecraft:custom": {
                      "minecraft:play_time": 1200,
                      "minecraft:deaths": 3
                    }
                  }
                }
                """).getAsJsonObject();

        var options = new EconomyLocalPlayerStatsReset.StatsResetOptions(
                true, false, false, false, false, false, false, false, false
        );
        assertTrue(EconomyLocalPlayerStatsReset.applyStatsReset(root, options));
        assertEquals(0, root.getAsJsonObject("stats")
                .getAsJsonObject("minecraft:custom")
                .get("minecraft:play_time").getAsInt());
        assertEquals(3, root.getAsJsonObject("stats")
                .getAsJsonObject("minecraft:custom")
                .get("minecraft:deaths").getAsInt());
    }

    @Test
    void applyStatsReset_zerosTravelDistanceKeys() {
        JsonObject root = JsonParser.parseString("""
                {
                  "stats": {
                    "minecraft:custom": {
                      "minecraft:walk_one_cm": 500,
                      "minecraft:sprint_one_cm": 200
                    }
                  }
                }
                """).getAsJsonObject();

        var options = new EconomyLocalPlayerStatsReset.StatsResetOptions(
                false, true, false, false, false, false, false, false, false
        );
        assertTrue(EconomyLocalPlayerStatsReset.applyStatsReset(root, options));
        var custom = root.getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
        assertEquals(0, custom.get("minecraft:walk_one_cm").getAsInt());
        assertEquals(0, custom.get("minecraft:sprint_one_cm").getAsInt());
    }

    @Test
    void applyStatsReset_resetsHarvestBlocksOnly() {
        JsonObject root = JsonParser.parseString("""
                {
                  "stats": {
                    "minecraft:mined": {
                      "minecraft:wheat": 10,
                      "minecraft:stone": 99
                    }
                  }
                }
                """).getAsJsonObject();

        var options = new EconomyLocalPlayerStatsReset.StatsResetOptions(
                false, false, false, false, false, false, true, false, false
        );
        assertTrue(EconomyLocalPlayerStatsReset.applyStatsReset(root, options));
        var mined = root.getAsJsonObject("stats").getAsJsonObject("minecraft:mined");
        assertEquals(0, mined.get("minecraft:wheat").getAsInt());
        assertEquals(99, mined.get("minecraft:stone").getAsInt());
    }

    @Test
    void applyStatsReset_noChangeWhenDisabled() {
        JsonObject root = JsonParser.parseString("""
                {
                  "stats": {
                    "minecraft:custom": {
                      "minecraft:play_time": 1200
                    }
                  }
                }
                """).getAsJsonObject();

        var options = new EconomyLocalPlayerStatsReset.StatsResetOptions(
                false, false, false, false, false, false, false, false, false
        );
        assertFalse(EconomyLocalPlayerStatsReset.applyStatsReset(root, options));
    }
}
