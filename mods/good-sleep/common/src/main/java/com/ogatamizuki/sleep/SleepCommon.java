package com.ogatamizuki.sleep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SleepCommon {
    public static final String MODID = "good_sleep";
    public static final Logger LOGGER = LogManager.getLogger("GoodSleep");

    public static boolean allowDaySleep = true;
    public static boolean healWhileSleeping = true;
    public static int healIntervalTicks = 20;
    public static boolean onePlayerSkip = false;

    private SleepCommon() {}
}
