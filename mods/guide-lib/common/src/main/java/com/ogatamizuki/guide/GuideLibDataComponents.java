package com.ogatamizuki.guide;

import net.minecraft.core.component.DataComponentType;
import java.util.function.Supplier;

public final class GuideLibDataComponents {
    public static Supplier<DataComponentType<GuideAccess>> GUIDE_ACCESS = () -> null;

    private GuideLibDataComponents() {}

    public static DataComponentType<GuideAccess> getGuideAccess() {
        return GUIDE_ACCESS != null ? GUIDE_ACCESS.get() : null;
    }
}
