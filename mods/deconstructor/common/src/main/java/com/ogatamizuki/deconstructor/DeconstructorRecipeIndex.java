package com.ogatamizuki.deconstructor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * クラフトレシピの成果物 Item からレシピ・素材リストへの逆引きインデックス。
 * RecipeManager が変わったときだけ全レシピを1回走査して構築する。
 */
public final class DeconstructorRecipeIndex {
    public record Entry(RecipeHolder<?> recipe, List<Ingredient> ingredients, int recipeOutputCount) {}

    private static final CraftingInput DUMMY_INPUT = CraftingInput.of(3, 3, List.of(
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    ));

    private static RecipeManager indexedManager;
    private static Map<Item, Entry> index = Map.of();

    private DeconstructorRecipeIndex() {}

    public static Entry lookup(RecipeManager recipeManager, Item item) {
        if (Config.isExcluded(item)) {
            return null;
        }
        ensureBuilt(recipeManager);
        return index.get(item);
    }

    public static void invalidate() {
        indexedManager = null;
        index = Map.of();
    }

    private static boolean containsOutputAsIngredient(Item resultItem, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) continue;
            try {
                @SuppressWarnings("deprecation")
                var holders = ingredient.items();
                if (holders != null && holders.anyMatch(holder -> holder != null && holder.value() == resultItem)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static void ensureBuilt(RecipeManager recipeManager) {
        if (recipeManager == indexedManager) {
            return;
        }
        synchronized (DeconstructorRecipeIndex.class) {
            if (recipeManager == indexedManager) {
                return;
            }
            Map<Item, Entry> newIndex = new HashMap<>();
            int totalChecked = 0;
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                var recipe = holder.value();
                boolean isCrafting = recipe instanceof CraftingRecipe cr && !cr.isSpecial();
                boolean isSmithing = recipe instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe;
                if (!isCrafting && !isSmithing) {
                    continue;
                }
                totalChecked++;
                ItemStack result = resolveResult(recipe);
                if (result.isEmpty()) {
                    continue;
                }
                Item resultItem = result.getItem();
                if (Config.isExcluded(resultItem)) {
                    continue;
                }
                if (newIndex.containsKey(resultItem)) {
                    continue;
                }
                List<Ingredient> ingredients = extractIngredients(recipe);
                if (ingredients.isEmpty()) {
                    continue;
                }
                if (containsOutputAsIngredient(resultItem, ingredients)) {
                    continue;
                }
                newIndex.put(resultItem, new Entry(holder, ingredients, result.getCount()));
            }
            index = Collections.unmodifiableMap(newIndex);
            indexedManager = recipeManager;
            DeconstructorCommon.LOGGER.info("Built deconstructor recipe index: {} recipes indexed from {} total candidate recipes",
                    index.size(), totalChecked);
        }
    }

    private static ItemStack resolveResult(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        // 1. Try display()
        try {
            for (var disp : recipe.display()) {
                if (disp instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
                    ItemStack stack = resolveFromSlotDisplay(shaped.result());
                    if (!stack.isEmpty()) return stack;
                } else if (disp instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
                    ItemStack stack = resolveFromSlotDisplay(shapeless.result());
                    if (!stack.isEmpty()) return stack;
                } else if (disp instanceof net.minecraft.world.item.crafting.display.SmithingRecipeDisplay smithing) {
                    ItemStack stack = resolveFromSlotDisplay(smithing.result());
                    if (!stack.isEmpty()) return stack;
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Try assemble(DUMMY_INPUT) for CraftingRecipe
        if (recipe instanceof CraftingRecipe craftingRecipe) {
            try {
                ItemStack stack = craftingRecipe.assemble(DUMMY_INPUT);
                if (!stack.isEmpty()) {
                    return stack;
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Reflection fallback searching for 'result' field in class hierarchy
        Class<?> clazz = recipe.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                java.lang.reflect.Field resultField = clazz.getDeclaredField("result");
                resultField.setAccessible(true);
                Object val = resultField.get(recipe);
                if (val instanceof ItemStack is && !is.isEmpty()) {
                    return is;
                } else if (val != null) {
                    java.lang.reflect.Method createMethod = val.getClass().getMethod("create");
                    createMethod.setAccessible(true);
                    return (ItemStack) createMethod.invoke(val);
                }
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack resolveFromSlotDisplay(net.minecraft.world.item.crafting.display.SlotDisplay display) {
        if (display instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay isd) {
            try {
                return isd.stack().create();
            } catch (Exception ignored) {
            }
        } else if (display instanceof net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay isd) {
            try {
                return new ItemStack(isd.item().value());
            } catch (Exception ignored) {
            }
        } else if (display instanceof net.minecraft.world.item.crafting.display.SlotDisplay.TagSlotDisplay tsd) {
            try {
                for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tsd.tag())) {
                    if (holder != null && holder.value() != null) {
                        return new ItemStack(holder.value());
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (display instanceof net.minecraft.world.item.crafting.display.SlotDisplay.Composite comp) {
            for (var sub : comp.contents()) {
                ItemStack res = resolveFromSlotDisplay(sub);
                if (!res.isEmpty()) return res;
            }
        } else if (display instanceof net.minecraft.world.item.crafting.display.SlotDisplay.WithRemainder withRem) {
            return resolveFromSlotDisplay(withRem.input());
        }
        return ItemStack.EMPTY;
    }

    private static List<Ingredient> extractIngredients(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        PlacementInfo placement = recipe.placementInfo();
        if (placement == null || placement.isImpossibleToPlace()) {
            return List.of();
        }
        return placement.ingredients();
    }

    public static List<ItemStack> previewStacksFor(Ingredient ingredient) {
        if (ingredient == null) {
            return List.of();
        }

        // 1. Try display() (handles TagSlotDisplay, ItemSlotDisplay, ItemStackSlotDisplay, Composite)
        try {
            var display = ingredient.display();
            ItemStack stack = resolveFromSlotDisplay(display);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                return List.of(copy);
            }
        } catch (Exception ignored) {
        }

        // 2. Try ingredient.items()
        try {
            @SuppressWarnings("deprecation")
            var holders = ingredient.items();
            List<ItemStack> items = new ArrayList<>();
            holders.forEach(holder -> {
                if (holder != null && holder.value() != null) {
                    items.add(new ItemStack(holder.value()));
                }
            });
            if (!items.isEmpty()) {
                ItemStack stack = items.getFirst().copy();
                stack.setCount(1);
                return List.of(stack);
            }
        } catch (Exception ignored) {
        }

        // 3. Try direct getValues()
        try {
            var values = ingredient.getValues();
            if (values != null) {
                for (var holder : values) {
                    if (holder != null && holder.value() != null) {
                        ItemStack stack = new ItemStack(holder.value());
                        stack.setCount(1);
                        return List.of(stack);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return List.of();
    }
}
