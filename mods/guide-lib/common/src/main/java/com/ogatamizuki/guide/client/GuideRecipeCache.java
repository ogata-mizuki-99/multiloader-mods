package com.ogatamizuki.guide.client;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.GuideModResourceScanner;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side crafting recipe cache. Dedicated Server clients do not receive a full RecipeManager from
 * the server, so crafting guide pages resolve recipes from local mod JAR JSON as well as reload listeners.
 */
public final class GuideRecipeCache {
    public static final Identifier LISTENER_ID = GuideLibCommon.id("guide_recipes");

    private static RecipeManager recipeManager;
    private static Map<Identifier, RecipeHolder<CraftingRecipe>> modJarRecipes = Map.of();

    private GuideRecipeCache() {}

    public static RecipeManager createRecipeManager() {
        recipeManager = new RecipeManager(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        loadFromModJars();
        return recipeManager;
    }

    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public static void invalidateModJarCache() {
        modJarRecipes = Map.of();
        loadFromModJars();
    }

    public static void ensureLoaded() {
        if (modJarRecipes.isEmpty()) {
            loadFromModJars();
        }
    }

    public static RecipeHolder<CraftingRecipe> get(Identifier recipeId) {
        ensureLoaded();

        RecipeHolder<CraftingRecipe> fromJar = modJarRecipes.get(recipeId);
        if (fromJar != null) {
            return fromJar;
        }

        if (recipeManager == null) {
            return null;
        }

        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);
        return recipeManager.byKey(recipeKey)
                .filter(holder -> holder.value() instanceof CraftingRecipe)
                .map(holder -> {
                    @SuppressWarnings("unchecked")
                    RecipeHolder<CraftingRecipe> typed = (RecipeHolder<CraftingRecipe>) holder;
                    return typed;
                })
                .orElse(null);
    }

    private static void loadFromModJars() {
        Map<Identifier, RecipeHolder<CraftingRecipe>> loaded = new LinkedHashMap<>();
        RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        GuideModResourceScanner.scanRecipeJson().forEach((recipeId, json) -> {
            try {
                Recipe<?> recipe = Recipe.CODEC.parse(ops, json).getOrThrow();
                if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
                    return;
                }
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipeId);
                loaded.put(recipeId, new RecipeHolder<>(key, craftingRecipe));
            } catch (Exception e) {
                GuideLibCommon.LOGGER.error("Failed to parse recipe {} from mod jar", recipeId, e);
            }
        });

        modJarRecipes = Collections.unmodifiableMap(loaded);
        GuideLibCommon.LOGGER.info("Indexed {} crafting recipe(s) from mod jars", loaded.size());
    }
}
