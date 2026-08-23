package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class InstantStructureAPI {
    /**
     * 指定されたテンプレートをワールドの座標に自動生成します。
     *
     * @param level 生成対象のサーバーレベル
     * @param category テンプレートのカテゴリー ("houses" または "arenas" または "custom")
     * @param templateName テンプレート名（NBTファイル名）
     * @param pos 生成基準座標
     * @param rotation 回転（0, 90, 180, 270）
     * @return 建築が成功した場合は true、失敗した場合は false
     */
    public static boolean buildTemplate(ServerLevel level, String category, String templateName, BlockPos pos, int rotation) {
        return InstantStructureMod.buildTemplateInternal(level, category, templateName, pos, rotation);
    }

    /**
     * テンプレートの JSON メタデータを読み込みます。
     */
    public static Optional<TemplateMetadata> readTemplateMetadata(String category, String templateName) {
        return InstantStructureMod.readTemplateMetadata(category, templateName);
    }

    /**
     * メタデータに定義された特殊座標（例: door_position, head_sign_position）を取得します。
     */
    public static Optional<BlockPos> getSpecialPosition(String category, String templateName, String key) {
        return readTemplateMetadata(category, templateName)
                .map(TemplateMetadata::specialPositions)
                .map(positions -> positions.get(key));
    }

    /**
     * NBT テンプレートのサイズ（ブロック数）を返します。
     */
    public static Optional<Vec3i> getTemplateSize(ServerLevel level, String category, String templateName) {
        return InstantStructureMod.getTemplateSize(level, category, templateName);
    }

    /**
     * 指定カテゴリのテンプレート名一覧（NBT ファイル名、拡張子なし）。
     */
    public static List<String> listTemplateNames(String category) {
        return InstantStructureMod.listTemplateNames(category);
    }

    /**
     * werewolf 用の家テンプレートとして利用可能か（NBT 存在 + 必須 special_positions）。
     */
    /** スプレッド建築（大規模テンプレの非同期配置）が進行中か。 */
    public static boolean hasActiveSpreadBuilds() {
        return SpreadBuildManager.hasActiveTasks();
    }

    public static boolean isWerewolfReadyHouse(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return false;
        }
        Path nbtPath = FMLPaths.CONFIGDIR.get().resolve("instant-structure")
                .resolve("templates-structure")
                .resolve("houses")
                .resolve(templateName + ".nbt");
        if (!Files.exists(nbtPath)) {
            return false;
        }
        return readTemplateMetadata("houses", templateName)
                .map(TemplateMetadata::specialPositions)
                .map(WerewolfHouseMetadata::isComplete)
                .orElse(false);
    }
}
