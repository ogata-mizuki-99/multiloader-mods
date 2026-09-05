package com.ogatamizuki.instantstructure.client;

public final class PreviewPerformancePolicy {
    /** この固体ブロック数以上で「大規模プレビュー」扱い */
    public static final int LARGE_BLOCK_THRESHOLD = 1500;
    /** 仮確定後の詳細ゴースト: カメラからこの距離（ブロック）以内のみ描画 */
    public static final double DETAILED_GHOST_RADIUS = 48.0;
    /** 1フレームあたりの詳細ゴースト描画上限（距離内でも打ち切り） */
    public static final int MAX_DETAILED_GHOSTS_PER_FRAME = 2500;

    private PreviewPerformancePolicy() {
    }

    public static boolean isLargePreview(int solidBlockCount) {
        return solidBlockCount >= LARGE_BLOCK_THRESHOLD;
    }

    public static double detailedGhostRadiusSquared() {
        double radius = DETAILED_GHOST_RADIUS;
        return radius * radius;
    }
}
