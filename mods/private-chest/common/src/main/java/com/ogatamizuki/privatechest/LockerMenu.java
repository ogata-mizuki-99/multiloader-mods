package com.ogatamizuki.privatechest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LockerMenu extends AbstractContainerMenu {
    private static final java.lang.reflect.Field SLOT_X;
    private static final java.lang.reflect.Field SLOT_Y;

    static {
        java.lang.reflect.Field x = null;
        java.lang.reflect.Field y = null;
        try {
            x = Slot.class.getDeclaredField("x");
            y = Slot.class.getDeclaredField("y");
            x.setAccessible(true);
            y.setAccessible(true);
        } catch (Exception e) {
            PrivateChestCommon.LOGGER.error("Failed to initialize Slot reflection fields in LockerMenu", e);
        }
        SLOT_X = x;
        SLOT_Y = y;
    }

    private final Container container;
    private final LockerBlockEntity blockEntity;

    public static final int VISIBLE_ROWS = 5;   // 画面に表示する行数
    public static final int TOTAL_ROWS   = 9;   // 全行数
    public static final int COLS         = 9;

    public static final int LOCKER_SLOT_START_Y = 18;
    public static final int PLAYER_INV_Y = LOCKER_SLOT_START_Y + VISIBLE_ROWS * 18 + 14;
    public static final int HOTBAR_Y     = PLAYER_INV_Y + 3 * 18 + 4;

    // Client-side constructor via IMenuTypeExtension
    public LockerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, (LockerBlockEntity) playerInventory.player.level().getBlockEntity(pos));
    }

    public LockerMenu(int containerId, Inventory playerInventory, LockerBlockEntity blockEntity) {
        super(PrivateChestCommon.LOCKER_MENU_TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity : new SimpleContainer(LockerBlockEntity.SLOT_COUNT);

        // ロッカースロット（全81スロット）
        // スクロールはScreenからslotのx/yを直接変更して制御する
        for (int r = 0; r < TOTAL_ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                this.addSlot(new Slot(this.container, r * COLS + c,
                        8 + c * 18, LOCKER_SLOT_START_Y + r * 18));
            }
        }

        // プレイヤーインベントリ
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, PLAYER_INV_Y + r * 18));
            }
        }

        // ホットバー
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInventory, c, 8 + c * 18, HOTBAR_Y));
        }

        // 初期スクロール位置を適用（行5〜8を画面外に追い出す）
        scrollTo(0);

        if (this.blockEntity != null && !playerInventory.player.level().isClientSide()) {
            this.blockEntity.startOpen(playerInventory.player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.blockEntity != null && !player.level().isClientSide()) {
            this.blockEntity.stopOpen(player);
        }
    }

    /**
     * スクロール行を適用してロッカースロットのY座標をリフレクションで変更する。
     * 表示範囲外スロットは x=-2000 に移動してクリックを無効化する。
     */
    public void scrollTo(int topRow) {
        topRow = Math.max(0, Math.min(topRow, TOTAL_ROWS - VISIBLE_ROWS));
        if (SLOT_X == null || SLOT_Y == null) {
            return;
        }
        try {
            for (int r = 0; r < TOTAL_ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    Slot slot = this.slots.get(r * COLS + c);
                    int displayRow = r - topRow;
                    if (displayRow >= 0 && displayRow < VISIBLE_ROWS) {
                        SLOT_X.set(slot, 8 + c * 18);
                        SLOT_Y.set(slot, LOCKER_SLOT_START_Y + displayRow * 18);
                    } else {
                        SLOT_X.set(slot, -2000);
                        SLOT_Y.set(slot, LOCKER_SLOT_START_Y);
                    }
                }
            }
        } catch (Exception e) {
            PrivateChestCommon.LOGGER.error("Failed to scroll locker slots", e);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();
            if (index < LockerBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(itemStack1, LockerBlockEntity.SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack1, 0, LockerBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
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
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public LockerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
