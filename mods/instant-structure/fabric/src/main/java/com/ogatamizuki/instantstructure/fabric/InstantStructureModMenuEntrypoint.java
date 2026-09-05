package com.ogatamizuki.instantstructure.fabric;

import com.ogatamizuki.instantstructure.client.InstantStructureConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class InstantStructureModMenuEntrypoint implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new InstantStructureConfigScreen(parent);
    }
}
