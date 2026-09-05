package com.ogatamizuki.deconstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class EnchantmentManagerMenu extends AbstractContainerMenu {
    private final EnchantmentManagerBlockEntity blockEntity;
    private final Level level;
    private boolean isUpdating = false;

    // 0: Input 1, 1: Input 2, 2: Output
    private final SimpleContainer container = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantmentManagerMenu.this.slotsChanged(this);
        }
    };

    public EnchantmentManagerMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(pos) instanceof EnchantmentManagerBlockEntity be ? be : null);
    }

    public EnchantmentManagerMenu(int containerId, Inventory playerInventory, EnchantmentManagerBlockEntity blockEntity) {
        super(DeconstructorBlocks.ENCHANT_MANAGER_MENU_TYPE.get(), containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();

        if (!level.isClientSide() && blockEntity != null) {
            this.container.setItem(0, blockEntity.getItem(0).copy());
            this.container.setItem(1, blockEntity.getItem(1).copy());
        }

        // Slots
        this.addSlot(new Slot(container, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        this.addSlot(new Slot(container, 1, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            }
        });

        this.addSlot(new Slot(container, 2, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !getItem().isEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                EnchantmentManagerMenu.this.onOutputTaken(player, stack);
            }
        });

        // Player Inventory
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }

        // Hotbar
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInventory, c, 8 + c * 18, 142));
        }

        if (!level.isClientSide()) {
            updateOutput();
        }
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (!level.isClientSide() && !isUpdating) {
            blockEntity.setItemIfChanged(0, this.container.getItem(0).copy());
            blockEntity.setItemIfChanged(1, this.container.getItem(1).copy());
            updateOutput();
        }
    }

    private void updateOutput() {
        if (level.isClientSide()) return;
        isUpdating = true;

        ItemStack slot0 = this.container.getItem(0);
        ItemStack slot1 = this.container.getItem(1);

        if (slot0.isEmpty() || slot1.isEmpty()) {
            this.container.setItem(2, ItemStack.EMPTY);
            isUpdating = false;
            return;
        }

        ItemStack output = ItemStack.EMPTY;

        // Case 1: Enchanted Item + Book
        if (!slot0.is(Items.ENCHANTED_BOOK) && !slot0.is(Items.BOOK) && slot0.has(DataComponents.ENCHANTMENTS) && slot1.is(Items.BOOK)) {
            ItemEnchantments enchants = slot0.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchants.isEmpty()) {
                output = new ItemStack(Items.ENCHANTED_BOOK);
                output.set(DataComponents.STORED_ENCHANTMENTS, enchants);
            }
        }
        // Case 2: Multi-enchanted Book + Book
        else if (slot0.is(Items.ENCHANTED_BOOK) && slot1.is(Items.BOOK)) {
            ItemEnchantments enchants = slot0.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchants.isEmpty()) {
                var iterator = enchants.entrySet().iterator();
                if (iterator.hasNext()) {
                    var entry = iterator.next();
                    output = new ItemStack(Items.ENCHANTED_BOOK);
                    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    mutable.set(entry.getKey(), entry.getIntValue());
                    output.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                }
            }
        }
        // Case 3: Enchanted Book + Enchanted Book
        else if (slot0.is(Items.ENCHANTED_BOOK) && slot1.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchants0 = slot0.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments enchants1 = slot1.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchants0.isEmpty() || !enchants1.isEmpty()) {
                ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(enchants0);
                for (var entry : enchants1.entrySet()) {
                    Holder<Enchantment> ench = entry.getKey();
                    int lvl1 = entry.getIntValue();
                    int lvl0 = merged.getLevel(ench);
                    if (lvl0 == lvl1) {
                        int maxLevel = ench.value().getMaxLevel();
                        merged.set(ench, Math.min(maxLevel, lvl0 + 1));
                    } else {
                        merged.set(ench, Math.max(lvl0, lvl1));
                    }
                }
                output = new ItemStack(Items.ENCHANTED_BOOK);
                output.set(DataComponents.STORED_ENCHANTMENTS, merged.toImmutable());
            }
        }
        // Case 4: Item + Enchanted Book
        else if (!slot0.is(Items.ENCHANTED_BOOK) && !slot0.is(Items.BOOK) && slot1.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments itemEnchants = slot0.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments bookEnchants = slot1.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!bookEnchants.isEmpty()) {
                ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(itemEnchants);
                for (var entry : bookEnchants.entrySet()) {
                    Holder<Enchantment> ench = entry.getKey();
                    int lvlBook = entry.getIntValue();
                    int lvlItem = merged.getLevel(ench);
                    if (lvlItem == lvlBook) {
                        int maxLevel = ench.value().getMaxLevel();
                        merged.set(ench, Math.min(maxLevel, lvlItem + 1));
                    } else {
                        merged.set(ench, Math.max(lvlItem, lvlBook));
                    }
                }
                output = slot0.copy();
                output.set(DataComponents.ENCHANTMENTS, merged.toImmutable());
            }
        }

        this.container.setItem(2, output);
        isUpdating = false;
    }

    private void onOutputTaken(Player player, ItemStack takenStack) {
        if (level.isClientSide()) return;
        isUpdating = true;

        ItemStack slot0 = this.container.getItem(0);
        ItemStack slot1 = this.container.getItem(1);

        // Case 1: Enchanted Item + Book -> Get Enchanted Book, return stripped Item and consume Book
        if (!slot0.is(Items.ENCHANTED_BOOK) && !slot0.is(Items.BOOK) && slot0.has(DataComponents.ENCHANTMENTS) && slot1.is(Items.BOOK)) {
            ItemStack stripped = slot0.copy();
            stripped.remove(DataComponents.ENCHANTMENTS);
            stripped.remove(DataComponents.REPAIR_COST); // Remove repair cost to allow full re-enchanting
            this.container.setItem(0, stripped);
            blockEntity.setItemIfChanged(0, stripped.copy());

            slot1.shrink(1);
            this.container.setItem(1, slot1);
            blockEntity.setItemIfChanged(1, slot1.copy());
        }
        // Case 2: Multi-enchanted Book + Book -> Get first Enchantment, remove it from source Book, consume Book
        else if (slot0.is(Items.ENCHANTED_BOOK) && slot1.is(Items.BOOK)) {
            ItemEnchantments enchants = slot0.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchants);
            var iterator = enchants.entrySet().iterator();
            if (iterator.hasNext()) {
                var entry = iterator.next();
                mutable.set(entry.getKey(), 0); // remove this enchantment
            }

            ItemEnchantments remaining = mutable.toImmutable();
            if (remaining.isEmpty()) {
                ItemStack book = new ItemStack(Items.BOOK);
                this.container.setItem(0, book);
                blockEntity.setItemIfChanged(0, book);
            } else {
                ItemStack updatedBook = slot0.copy();
                updatedBook.set(DataComponents.STORED_ENCHANTMENTS, remaining);
                this.container.setItem(0, updatedBook);
                blockEntity.setItemIfChanged(0, updatedBook.copy());
            }

            slot1.shrink(1);
            this.container.setItem(1, slot1);
            blockEntity.setItemIfChanged(1, slot1.copy());
        }
        // Case 3: Enchanted Book + Enchanted Book -> Merged Book, consume both Books
        else if (slot0.is(Items.ENCHANTED_BOOK) && slot1.is(Items.ENCHANTED_BOOK)) {
            slot0.shrink(1);
            this.container.setItem(0, slot0);
            blockEntity.setItemIfChanged(0, slot0.copy());

            slot1.shrink(1);
            this.container.setItem(1, slot1);
            blockEntity.setItemIfChanged(1, slot1.copy());
        }
        // Case 4: Item + Enchanted Book -> Merged Item, consume Item (since merged is output), replace Book with empty Book
        else if (!slot0.is(Items.ENCHANTED_BOOK) && !slot0.is(Items.BOOK) && slot1.is(Items.ENCHANTED_BOOK)) {
            slot0.shrink(1);
            this.container.setItem(0, slot0);
            blockEntity.setItemIfChanged(0, slot0.copy());

            ItemStack emptyBook = new ItemStack(Items.BOOK);
            this.container.setItem(1, emptyBook);
            blockEntity.setItemIfChanged(1, emptyBook);
        }

        isUpdating = false;
        updateOutput();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();

            if (index == 2) {
                // From Output to Player Inventory
                if (!this.moveItemStackTo(itemStack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack1, itemStack);
            } else if (index == 0 || index == 1) {
                // From Input slots to Player Inventory
                if (!this.moveItemStackTo(itemStack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From Player Inventory to Input slots
                if (itemStack1.is(Items.BOOK) || itemStack1.is(Items.ENCHANTED_BOOK)) {
                    if (!this.moveItemStackTo(itemStack1, 1, 2, false)) { // try slot 1 (Book slot)
                        if (!this.moveItemStackTo(itemStack1, 0, 1, false)) { // try slot 0
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(itemStack1, 0, 1, false)) { // try slot 0
                        return ItemStack.EMPTY;
                    }
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
        if (!level.isClientSide() && blockEntity != null) {
            blockEntity.setItemIfChanged(0, this.container.getItem(0).copy());
            blockEntity.setItemIfChanged(1, this.container.getItem(1).copy());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null || player.distanceToSqr(
                (double)blockEntity.getBlockPos().getX() + 0.5,
                (double)blockEntity.getBlockPos().getY() + 0.5,
                (double)blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
