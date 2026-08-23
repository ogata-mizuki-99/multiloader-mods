package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;

public class Selection {
    private BlockPos pos1;
    private BlockPos pos2;
    private boolean confirmed;
    private boolean exportPending;

    public Selection(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public boolean hasStart() {
        return pos1 != null;
    }

    public boolean hasBoth() {
        return pos1 != null && pos2 != null;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isExportPending() {
        return exportPending;
    }

    public void setExportPending(boolean exportPending) {
        this.exportPending = exportPending;
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
        confirmed = false;
        exportPending = false;
    }

    /**
     * Shift+右クリックで1段階戻す。
     * 確定済み → 終点のみ / 始点のみ → 未設定 / 未設定 → 変化なし
     *
     * @return 状態が変化した場合 true
     */
    public boolean undoStep() {
        if (exportPending || confirmed) {
            exportPending = false;
            confirmed = false;
            return true;
        }
        if (pos2 != null) {
            pos2 = null;
            return true;
        }
        if (pos1 != null) {
            pos1 = null;
            return true;
        }
        return false;
    }

    public void expand(String direction, int amount) {
        if (!hasBoth()) {
            return;
        }
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        switch (direction.toLowerCase()) {
            case "up" -> maxY += amount;
            case "down" -> minY -= amount;
            case "north" -> minZ -= amount;
            case "south" -> maxZ += amount;
            case "west" -> minX -= amount;
            case "east" -> maxX += amount;
            default -> {
                return;
            }
        }

        pos1 = new BlockPos(minX, minY, minZ);
        pos2 = new BlockPos(maxX, maxY, maxZ);
        confirmed = false;
        exportPending = false;
    }

    public void adjustCeiling(int delta) {
        if (!hasBoth()) {
            return;
        }
        int maxY = Math.max(pos1.getY(), pos2.getY()) + delta;
        if (pos1.getY() >= pos2.getY()) {
            pos1 = new BlockPos(pos1.getX(), maxY, pos1.getZ());
        } else {
            pos2 = new BlockPos(pos2.getX(), maxY, pos2.getZ());
        }
        confirmed = false;
        exportPending = false;
    }

    public void adjustStartY(int delta) {
        if (!hasStart() || pos1 == null || hasBoth()) {
            return;
        }
        pos1 = pos1.offset(0, delta, 0);
        confirmed = false;
        exportPending = false;
    }
}
