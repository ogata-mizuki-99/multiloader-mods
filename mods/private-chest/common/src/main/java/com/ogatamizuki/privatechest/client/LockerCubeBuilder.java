package com.ogatamizuki.privatechest.client;

import com.google.common.collect.Lists;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.core.Direction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Builds {@link CubeListBuilder} cubes with HD tex scale and per-face visibility.
 */
final class LockerCubeBuilder {
    private static final Constructor<CubeDefinition> CUBE_DEFINITION_CONSTRUCTOR;

    static {
        try {
            Constructor<CubeDefinition> constructor = CubeDefinition.class.getDeclaredConstructor(
                    String.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    CubeDeformation.class,
                    boolean.class,
                    float.class,
                    float.class,
                    Set.class
            );
            constructor.setAccessible(true);
            CUBE_DEFINITION_CONSTRUCTOR = constructor;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private LockerCubeBuilder() {
    }

    static CubeListBuilder scaledVisible(
            int u,
            int v,
            float x,
            float y,
            float z,
            float w,
            float h,
            float d,
            boolean mirror,
            float texScale,
            Set<Direction> visible
    ) {
        CubeDefinition cube;
        try {
            cube = CUBE_DEFINITION_CONSTRUCTOR.newInstance(
                    null,
                    (float) u,
                    (float) v,
                    x,
                    y,
                    z,
                    w,
                    h,
                    d,
                    CubeDeformation.NONE,
                    mirror,
                    texScale,
                    texScale,
                    visible
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create locker cube definition", exception);
        }

        CubeListBuilder builder = CubeListBuilder.create();
        try {
            Field cubesField = CubeListBuilder.class.getDeclaredField("cubes");
            cubesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<CubeDefinition> cubes = (List<CubeDefinition>) cubesField.get(builder);
            if (cubes == null) {
                cubes = Lists.newArrayList();
                cubesField.set(builder, cubes);
            }
            cubes.add(cube);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to build locker cube", exception);
        }
        return builder;
    }
}
