package com.ogatamizuki.economy.client;

import net.minecraft.client.Minecraft;
import java.util.Locale;

public class ClientLocaleHelper {
    public static Locale getClientLocale() {
        try {
            String langCode = Minecraft.getInstance().getLanguageManager().getSelected();
            if (langCode != null) {
                String[] parts = langCode.split("_");
                if (parts.length == 2) {
                    return new Locale(parts[0], parts[1].toUpperCase(Locale.ROOT));
                } else if (parts.length == 1) {
                    return new Locale(parts[0]);
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        return Locale.getDefault();
    }
}
