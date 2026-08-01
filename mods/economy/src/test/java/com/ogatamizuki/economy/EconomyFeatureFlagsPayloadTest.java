package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyFeatureFlagsPayloadTest {

    @Test
    void streamCodec_roundTripsAllFieldsIncludingAggregateSeconds() {
        EconomyFeatureFlagsPayload original = new EconomyFeatureFlagsPayload(true, false, true, 7);

        ByteBuf buffer = Unpooled.buffer();
        EconomyFeatureFlagsPayload.STREAM_CODEC.encode(buffer, original);
        buffer.readerIndex(0);
        EconomyFeatureFlagsPayload decoded = EconomyFeatureFlagsPayload.STREAM_CODEC.decode(buffer);

        assertEquals(original, decoded);
        assertTrue(decoded.enableBalanceHud());
        assertFalse(decoded.enableActionRewards());
        assertTrue(decoded.enableEtfUpdates());
        assertEquals(7, decoded.rewardChatAggregateSeconds());
    }
}
