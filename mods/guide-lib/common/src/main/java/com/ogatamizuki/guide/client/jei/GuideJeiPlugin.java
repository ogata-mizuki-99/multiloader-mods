package com.ogatamizuki.guide.client.jei;

import com.ogatamizuki.guide.GuideLibCommon;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class GuideJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return GuideLibCommon.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        GuideJeiClient.setRuntime(jeiRuntime);
    }
}
