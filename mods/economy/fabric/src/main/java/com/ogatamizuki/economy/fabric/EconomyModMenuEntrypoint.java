package com.ogatamizuki.economy.fabric;

import com.ogatamizuki.economy.*;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class EconomyModMenuEntrypoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EconomyConfigScreenFabric::new;
    }
}
