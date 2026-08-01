package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyAdminActionPayloadTest {

    @Test
    void forAction_createsPayloadWithNoResetFlags() {
        EconomyAdminActionPayload payload = EconomyAdminActionPayload.forAction("COMPILE_RANKING", 3);
        assertEquals("COMPILE_RANKING", payload.action());
        assertEquals(3, payload.shopId());
        assertFalse(payload.resetBalances());
        assertFalse(payload.resetPlayTime());
    }

    @Test
    void streamCodec_roundTripsAllResetFlags() {
        EconomyAdminActionPayload original = new EconomyAdminActionPayload(
                "RESET",
                true, false, true, false, true, false, true, false,
                true, false, true, false, true, false, true, false,
                0
        );

        ByteBuf buffer = Unpooled.buffer();
        EconomyAdminActionPayload.STREAM_CODEC.encode(buffer, original);
        buffer.readerIndex(0);
        EconomyAdminActionPayload decoded = EconomyAdminActionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(original, decoded);
    }

    @Test
    void decodePayload_readsTravelDistanceFlag() {
        EconomyAdminActionPayload decoded = EconomyAdminActionPayload.decodePayload("RESET", 1 << 8, 0);
        assertTrue(decoded.resetTravelDistance());
        assertFalse(decoded.resetBalances());
    }

    @Test
    void streamCodec_roundTripsSingleFlag() {
        EconomyAdminActionPayload original = EconomyAdminActionPayload.decodePayload("RESET", 1 << 8, 0);

        ByteBuf buffer = Unpooled.buffer();
        EconomyAdminActionPayload.STREAM_CODEC.encode(buffer, original);
        buffer.readerIndex(0);
        EconomyAdminActionPayload decoded = EconomyAdminActionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(original, decoded);
    }
}
