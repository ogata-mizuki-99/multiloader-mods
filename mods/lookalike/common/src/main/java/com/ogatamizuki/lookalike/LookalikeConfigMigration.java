package com.ogatamizuki.lookalike;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migrates renamed config keys in {@code lookalike-common.toml} before NeoForge loads the spec.
 */
public final class LookalikeConfigMigration {
    private static final String CONFIG_FILE = LookalikeCommon.MODID + "-common.toml";
    private static final String OLD_KEY = "hideDisguisedNametags";
    private static final String NEW_KEY = "hideAllNametags";
    private static final Pattern LEGACY_KEY_PATTERN = Pattern.compile("^\\s*" + OLD_KEY + "\\s*=\\s*(true|false)\\s*(#.*)?$");

    private LookalikeConfigMigration() {
    }

    public static void migrateConfigFile() {
        Path configPath = Path.of("config").resolve(CONFIG_FILE);
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            if (lines.stream().noneMatch(line -> line.trim().startsWith(OLD_KEY + " ="))) {
                return;
            }
            if (lines.stream().anyMatch(line -> line.trim().startsWith(NEW_KEY + " ="))) {
                removeLegacyKey(lines);
            } else {
                replaceLegacyKey(lines);
            }
            Files.write(configPath, lines, StandardCharsets.UTF_8);
            LookalikeCommon.LOGGER.info("Migrated lookalike config key {} -> {}", OLD_KEY, NEW_KEY);
        } catch (IOException e) {
            LookalikeCommon.LOGGER.warn("Failed to migrate lookalike config file {}", configPath, e);
        }
    }

    private static void replaceLegacyKey(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = LEGACY_KEY_PATTERN.matcher(lines.get(i));
            if (!matcher.matches()) {
                continue;
            }
            String suffix = matcher.group(2) != null ? " " + matcher.group(2).trim() : "";
            lines.set(i, NEW_KEY + " = " + matcher.group(1) + suffix);
            return;
        }
    }

    private static void removeLegacyKey(List<String> lines) {
        List<String> filtered = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (!LEGACY_KEY_PATTERN.matcher(line).matches()) {
                filtered.add(line);
            }
        }
        lines.clear();
        lines.addAll(filtered);
    }
}
