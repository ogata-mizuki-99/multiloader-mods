package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.client.GuideRecipeCache;
import com.ogatamizuki.guide.client.jei.GuideJeiBridge;
import com.ogatamizuki.guide.client.screen.GuideItemTooltips;
import com.ogatamizuki.guide.client.screen.GuideUi;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public class CraftingPageRenderer implements PageRenderer {
    private static final int SLOT_SIZE = 18;
    private static final int GRID_SIZE = SLOT_SIZE * 3;
    private static final int GRID_RESULT_GAP = 24;
    private static final int RECIPE_WIDTH = GRID_SIZE + GRID_RESULT_GAP + SLOT_SIZE;

    private record InteractiveSlot(int x, int y, ItemStack stack, boolean result) {}

    private final List<InteractiveSlot> interactiveSlots = new ArrayList<>();

    @Override
    public void render(GuiGraphicsExtractor gui, GuideTheme theme, GuidePage page, int x, int y, int width, int height, int mouseX, int mouseY) {
        interactiveSlots.clear();
        if (!page.data().has("recipe")) {
            renderMissing(gui, theme, x, y, width);
            return;
        }

        Identifier recipeId;
        try {
            recipeId = Identifier.parse(page.data().get("recipe").getAsString());
        } catch (Exception e) {
            renderMissing(gui, theme, x, y, width);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RecipeHolder<CraftingRecipe> holder = findCraftingRecipe(mc, recipeId);
        if (holder == null) {
            renderMissing(gui, theme, x, y, width);
            return;
        }
        CraftingRecipe craftingRecipe = holder.value();

        Font font = mc.font;
        int labelHeight = font.lineHeight + 8;
        int blockHeight = labelHeight + GRID_SIZE;
        int naturalHeight = contentHeight(theme, page, width, height);
        int blockY = height > naturalHeight
                ? y + Math.max(0, (height - blockHeight) / 2)
                : y + 2;
        int gridX = x + (width - RECIPE_WIDTH) / 2;
        int gridY = blockY + labelHeight;
        int clipTop = y;
        int clipBottom = y + height;

        if (blockY + 2 + font.lineHeight > clipTop && blockY + 2 < clipBottom) {
            gui.text(font, Component.translatable("guide_lib.page.crafting"), x + 8, blockY + 2, theme.colorSubtitle(), false);
        }

        if (craftingRecipe instanceof ShapelessRecipe shapelessRecipe) {
            renderShapeless(gui, theme, shapelessRecipe, gridX, gridY, clipTop, clipBottom, mouseX, mouseY);
        } else {
            renderShaped(gui, theme, craftingRecipe, gridX, gridY, clipTop, clipBottom, mouseX, mouseY);
        }

        CraftingInput dummyInput = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        ItemStack result = craftingRecipe.assemble(dummyInput);
        if (result.isEmpty() && craftingRecipe instanceof ShapelessRecipe shapelessRecipe) {
            result = shapelessRecipe.result().create();
        }

        int resultX = gridX + GRID_SIZE + GRID_RESULT_GAP;
        int resultY = gridY + (GRID_SIZE - SLOT_SIZE) / 2;
        if (resultY + SLOT_SIZE > clipTop && resultY < clipBottom) {
            drawSlot(gui, theme, font, result, resultX, resultY, true, mouseX, mouseY);
        }
    }

    @Override
    public int contentHeight(GuideTheme theme, GuidePage page, int width, int height) {
        Font font = Minecraft.getInstance().font;
        return font.lineHeight + 8 + GRID_SIZE + 16;
    }

    @Override
    public boolean mouseClicked(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            GuidePage page,
            int x,
            int y,
            int width,
            int height,
            MouseButtonEvent event
    ) {
        if (interactiveSlots.isEmpty()) {
            return false;
        }
        int button = event.button();
        if (button != 0 && button != 1) {
            return false;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        for (InteractiveSlot slot : interactiveSlots) {
            if (!GuideItemTooltips.isMouseOver((int) mouseX, (int) mouseY, slot.x, slot.y, SLOT_SIZE)) {
                continue;
            }
            return GuideJeiBridge.handleItemClick(slot.stack, button);
        }
        return false;
    }

    private void drawSlot(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            Font font,
            ItemStack stack,
            int slotX,
            int slotY,
            boolean result,
            int mouseX,
            int mouseY
    ) {
        if (stack.isEmpty()) {
            GuideUi.drawItemSlot(theme, gui, slotX, slotY);
            return;
        }
        GuideUi.drawItemStack(theme, gui, stack, slotX, slotY);
        interactiveSlots.add(new InteractiveSlot(slotX, slotY, stack, result));
        GuideItemTooltips.renderItemTooltip(gui, font, stack, mouseX, mouseY, slotX, slotY, SLOT_SIZE);
    }

    private static RecipeHolder<CraftingRecipe> findCraftingRecipe(Minecraft mc, Identifier recipeId) {
        RecipeHolder<CraftingRecipe> fromCache = GuideRecipeCache.get(recipeId);
        if (fromCache != null) {
            return fromCache;
        }

        if (mc.hasSingleplayerServer()) {
            return lookupInManager(mc.getSingleplayerServer().getRecipeManager(), recipeId);
        }
        return null;
    }

    private static RecipeHolder<CraftingRecipe> lookupInManager(RecipeManager recipeManager, Identifier recipeId) {
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

    private void renderShaped(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            CraftingRecipe recipe,
            int gridX,
            int gridY,
            int clipTop,
            int clipBottom,
            int mouseX,
            int mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        PlacementInfo placement = recipe.placementInfo();
        if (placement == null || placement.isImpossibleToPlace()) {
            return;
        }
        List<Ingredient> ingredients = placement.ingredients();
        var slotMap = placement.slotsToIngredientIndex();

        for (int slot = 0; slot < 9; slot++) {
            int col = slot % 3;
            int row = slot / 3;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;
            if (slotY + SLOT_SIZE <= clipTop || slotY >= clipBottom) {
                continue;
            }

            if (slot >= slotMap.size()) {
                GuideUi.drawItemSlot(theme, gui, slotX, slotY);
                continue;
            }
            int ingredientIndex = slotMap.getInt(slot);
            if (ingredientIndex == PlacementInfo.EMPTY_SLOT || ingredientIndex < 0 || ingredientIndex >= ingredients.size()) {
                GuideUi.drawItemSlot(theme, gui, slotX, slotY);
                continue;
            }
            drawSlot(gui, theme, font, firstStack(ingredients.get(ingredientIndex)), slotX, slotY, false, mouseX, mouseY);
        }
    }

    private void renderShapeless(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            ShapelessRecipe recipe,
            int gridX,
            int gridY,
            int clipTop,
            int clipBottom,
            int mouseX,
            int mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        List<Ingredient> ingredients = recipe.placementInfo().ingredients();
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int slotX = gridX + col * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;
            if (slotY + SLOT_SIZE <= clipTop || slotY >= clipBottom) {
                continue;
            }
            if (i >= ingredients.size()) {
                GuideUi.drawItemSlot(theme, gui, slotX, slotY);
                continue;
            }
            drawSlot(gui, theme, font, firstStack(ingredients.get(i)), slotX, slotY, false, mouseX, mouseY);
        }
    }

    private static ItemStack firstStack(Ingredient ingredient) {
        @SuppressWarnings("deprecation")
        var holders = ingredient.items();
        var iterator = holders.iterator();
        if (!iterator.hasNext()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(iterator.next().value());
        stack.setCount(1);
        return stack;
    }

    private static void renderMissing(GuiGraphicsExtractor gui, GuideTheme theme, int x, int y, int width) {
        Font font = Minecraft.getInstance().font;
        gui.text(font, Component.translatable("guide_lib.page.recipe_missing"), x + 8, y + 24, theme.colorError(), false);
    }
}
