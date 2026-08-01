package com.ogatamizuki.deconstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public class DeconstructorMenu extends AbstractContainerMenu {
    private final DeconstructorBlockEntity blockEntity;
    private final Level level;

    // 内部的なスロットコンテナ
    // Slot 0: 入力スロット
    // Slot 1~9: プレビュー用スロット
    private final SimpleContainer container = new SimpleContainer(10) {
        @Override
        public void setChanged() {
            super.setChanged();
            DeconstructorMenu.this.slotsChanged(this);
        }
    };

    private final int maxExtractCount;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.SimpleContainerData(2);
    private boolean isUpdatingRecipe = false;

    public DeconstructorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, (DeconstructorBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public DeconstructorMenu(int containerId, Inventory playerInventory, DeconstructorBlockEntity blockEntity) {
        super(DeconstructorMod.DECONSTRUCTOR_MENU_TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.maxExtractCount = blockEntity.getMaxExtractCount();
        
        // クライアント・サーバー間でデータを同期
        this.addDataSlots(this.data);

        // サーバー側の場合、ブロックエンティティから入力アイテムをコピー
        if (!level.isClientSide()) {
            this.container.setItem(0, blockEntity.getInputStack().copy());
        }

        // 入力スロットの配置
        this.addSlot(new Slot(container, 0, 80, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }
        });

        // プレビュースロットの配置（1〜9スロット）
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                this.addSlot(new DeconstructorPreviewSlot(container, 1 + index, 62 + c * 18, 56 + r * 18, this, index));
            }
        }

        // プレイヤーインベントリ
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 120 + r * 18));
            }
        }

        // ホットバー
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInventory, c, 8 + c * 18, 178));
        }

        // 初期状態でレシピ適用
        if (!level.isClientSide()) {
            updateRecipePreview();
        }
    }

    public int getCurrentExtractCount() {
        return this.data.get(0);
    }

    public void setCurrentExtractCount(int val) {
        this.data.set(0, val);
    }

    public int getRecipeOutputCount() {
        return this.data.get(1);
    }

    public void setRecipeOutputCount(int val) {
        this.data.set(1, val);
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (!level.isClientSide() && !isUpdatingRecipe) {
            ItemStack input = this.container.getItem(0);
            ItemStack cached = blockEntity.getInputStack();

            // 入力スロットが空になった場合（ただし、回収がまだ始まっていない場合のみクリアする）
            if (input.isEmpty() && getCurrentExtractCount() == 0) {
                blockEntity.setInputStackIfChanged(ItemStack.EMPTY);
                setCurrentExtractCount(0);
                updateRecipePreview();
                return;
            }

            // 個数を無視してアイテムタイプとコンポーネントが同じか確認
            boolean isSame = ItemStack.isSameItemSameComponents(input, cached);
            
            // 入力スロットに新しい種類のアイテムが置かれた場合（空スロットから置かれた場合を含む）
            if (!input.isEmpty() && !isSame) {
                blockEntity.setInputStackIfChanged(input.copy());
                setCurrentExtractCount(0);
                updateRecipePreview();
                return;
            }
            
            // 既に回収開始（1つ以上取得）している間は、入力スロットの個数変更などではプレビュー状態を保持する
            if (getCurrentExtractCount() > 0) {
                blockEntity.setInputStackIfChanged(input.copy());
                return;
            }
            
            boolean previewEmpty = isPreviewEmpty(this.container);

            if (previewEmpty && !input.isEmpty()) {
                blockEntity.setInputStackIfChanged(input.copy());
                // 前回の分解が完了した場合、または必要数に達するまで補充した場合
                setCurrentExtractCount(0);
                updateRecipePreview();
            } else {
                // 個数のみの変更（消費など）
                blockEntity.setInputStackIfChanged(input.copy());
                // 必要数を下回ったら回収不能なプレビューを消す（必要数は GUI 表示用に保持）
                if (getCurrentExtractCount() == 0 && !input.isEmpty() && !hasEnoughInput(input)) {
                    isUpdatingRecipe = true;
                    clearPreviewSlots();
                    isUpdatingRecipe = false;
                }
            }
        }
    }

    private static boolean isPreviewEmpty(SimpleContainer container) {
        for (int i = 1; i <= 9; i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEnoughInput(ItemStack input) {
        if (input.isEmpty()) {
            return false;
        }
        return input.getCount() >= Math.max(1, getRecipeOutputCount());
    }

    // レシピを解決し、プレビュースロットを更新
    private void updateRecipePreview() {
        if (level.isClientSide()) return;
        isUpdatingRecipe = true;

        ItemStack input = this.container.getItem(0);
        if (input.isEmpty()) {
            setRecipeOutputCount(1);
            clearPreviewSlots();
            isUpdatingRecipe = false;
            return;
        }

        var server = level.getServer();
        if (server == null) {
            isUpdatingRecipe = false;
            return;
        }

        RecipeManager recipeManager = server.getRecipeManager();
        DeconstructorRecipeIndex.Entry entry = DeconstructorRecipeIndex.lookup(
                recipeManager,
                input.getItem()
        );

        if (entry != null) {
            int need = Math.max(1, entry.recipeOutputCount());
            setRecipeOutputCount(need);
            // 必要数未満のときはプレビューを出さない（回収不能な表示で連続解体が止まったように見えるのを防ぐ）
            if (input.getCount() >= need) {
                applyIngredientsToPreview(entry.ingredients());
            } else {
                clearPreviewSlots();
            }
        } else {
            setRecipeOutputCount(1);
            clearPreviewSlots();
        }

        isUpdatingRecipe = false;
    }

    private void clearPreviewSlots() {
        for (int i = 1; i <= 9; i++) {
            this.container.setItem(i, ItemStack.EMPTY);
        }
    }

    private void applyIngredientsToPreview(List<Ingredient> ingredients) {
        for (int i = 0; i < 9; i++) {
            if (i < ingredients.size()) {
                List<ItemStack> previewStacks = DeconstructorRecipeIndex.previewStacksFor(ingredients.get(i));
                this.container.setItem(1 + i, previewStacks.isEmpty() ? ItemStack.EMPTY : previewStacks.getFirst());
            } else {
                this.container.setItem(1 + i, ItemStack.EMPTY);
            }
        }
    }

    public boolean canExtract() {
        ItemStack input = container.getItem(0);
        int currentCount = getCurrentExtractCount();
        int reqCount = Math.max(1, getRecipeOutputCount());
        boolean hasInput = (currentCount > 0) || (!input.isEmpty() && input.getCount() >= reqCount);
        return hasInput && currentCount < maxExtractCount;
    }

    public void onPreviewTaken(int index, ItemStack takenStack) {
        if (level.isClientSide()) return;
        if (!canExtract()) return;

        boolean refreshPreview = false;
        // slotsChanged が消費・クリア途中の状態でプレビューを壊さないようにする
        isUpdatingRecipe = true;
        try {
            int currentCount = getCurrentExtractCount();
            int reqCount = Math.max(1, getRecipeOutputCount());

            // slotsChangedでのリセットを防ぐため、先に回収カウントを増加させる
            setCurrentExtractCount(currentCount + 1);
            currentCount++;

            // 入力アイテムを減らす（最初の抽出タイミングのみ必要数分消費）
            if (currentCount == 1) {
                ItemStack input = this.container.getItem(0);
                if (!input.isEmpty()) {
                    input.shrink(reqCount);
                    ItemStack remaining = input.isEmpty() ? ItemStack.EMPTY : input.copy();
                    this.container.setItem(0, remaining);
                    blockEntity.setInputStackIfChanged(remaining.copy());
                }
            }

            // 取り出されたプレビュースロットを空にする
            this.container.setItem(1 + index, ItemStack.EMPTY);

            // 回収上限に達した場合のみクリアし、残数があれば連続解体用に再プレビュー
            if (currentCount >= maxExtractCount) {
                clearPreviewSlots();
                setCurrentExtractCount(0);
                refreshPreview = true;
            }
        } finally {
            isUpdatingRecipe = false;
        }

        if (refreshPreview) {
            updateRecipePreview();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (index == 0) {
                // 入力スロットからプレイヤーインベントリへ
                if (!this.moveItemStackTo(itemStack1, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index <= 9) {
                // プレビュースロットのShiftクリック回収
                if (!level.isClientSide()) {
                    ItemStack clickedItem = slot.getItem();
                    if (!clickedItem.isEmpty()) {
                        int initialCount = getCurrentExtractCount();
                        int limit = this.maxExtractCount - initialCount;
                        int moved = 0;

                        // 対象と同じアイテムをプレビュースロット全体から探して移動する
                        // まずクリックされたスロットから優先して処理し、次に他のスロットを処理する
                        List<Integer> targetIndices = new ArrayList<>();
                        targetIndices.add(index);
                        for (int i = 1; i <= 9; i++) {
                            if (i != index) {
                                targetIndices.add(i);
                            }
                        }

                        for (int slotIdx : targetIndices) {
                            if (moved >= limit || !canExtract()) {
                                break;
                            }
                            Slot targetSlot = this.slots.get(slotIdx);
                            if (targetSlot != null && targetSlot.hasItem()) {
                                ItemStack targetItem = targetSlot.getItem();
                                if (ItemStack.isSameItemSameComponents(clickedItem, targetItem)) {
                                    ItemStack copyToGive = targetItem.copy();
                                    if (this.moveItemStackTo(copyToGive, 10, this.slots.size(), true)) {
                                        moved++;
                                        onPreviewTaken(slotIdx - 1, targetItem);
                                    }
                                }
                            }
                        }
                    }
                }
                return ItemStack.EMPTY;
            } else {
                // プレイヤーインベントリから入力スロットへ
                if (!this.moveItemStackTo(itemStack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide()) {
            // コンテナが閉じられた際、入力スロットの中身を保存
            blockEntity.setInputStackIfChanged(this.container.getItem(0).copy());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null && player.distanceToSqr(
                (double)blockEntity.getBlockPos().getX() + 0.5,
                (double)blockEntity.getBlockPos().getY() + 0.5,
                (double)blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    // プレビュースロット用のカスタムスロットクラス
    private static class DeconstructorPreviewSlot extends Slot {
        private final DeconstructorMenu menu;
        private final int index;

        public DeconstructorPreviewSlot(net.minecraft.world.Container container, int slotId, int x, int y, DeconstructorMenu menu, int index) {
            super(container, slotId, x, y);
            this.menu = menu;
            this.index = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // プレビュースロットには置けない
        }

        @Override
        public boolean mayPickup(Player player) {
            return menu.canExtract();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            // 消費・連続プレビュー更新を先に行い、super の setChanged で slotsChanged が中途半端な状態を見ないようにする
            menu.onPreviewTaken(this.index, stack);
            super.onTake(player, stack);
        }
    }
}
