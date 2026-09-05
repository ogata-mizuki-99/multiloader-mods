package com.ogatamizuki.economy.backend.local;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLocalAdminServiceTest {

    @Test
    void resetOptions_hasAny_falseWhenAllDisabled() {
        var options = new EconomyLocalAdminService.ResetOptions(
                false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false
        );
        assertFalse(options.hasAny());
    }

    @Test
    void resetOptions_hasAny_trueWhenAnyEnabled() {
        var options = new EconomyLocalAdminService.ResetOptions(
                false, false, false, false, false, false, false,
                true, false, false, false, false, false, false, false, false
        );
        assertTrue(options.hasAny());
    }

    @Test
    void resetOptions_statsResetOptions_mapsPlayTimeFlag() {
        var options = new EconomyLocalAdminService.ResetOptions(
                false, false, false, false, false, false, false,
                true, true, false, false, false, false, false, false, false
        );
        var stats = options.statsResetOptions();
        assertTrue(stats.resetPlayTime());
        assertTrue(stats.resetTravelDistance());
        assertFalse(stats.resetDeaths());
        assertTrue(stats.hasAny());
    }

    @Test
    void resetOptions_fullReset_hasAny() {
        assertTrue(EconomyLocalAdminService.ResetOptions.fullReset().hasAny());
        assertTrue(EconomyLocalAdminService.ResetOptions.fullReset().statsResetOptions().hasAny());
    }
}
