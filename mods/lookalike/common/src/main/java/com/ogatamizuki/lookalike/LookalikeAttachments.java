package com.ogatamizuki.lookalike;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LookalikeAttachments {
    public record ScanEntry(UUID uuid, String name) {
        public static final Codec<ScanEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(ScanEntry::uuid),
                Codec.STRING.fieldOf("name").forGetter(ScanEntry::name)
        ).apply(instance, ScanEntry::new));
    }

    public static class ScanHistory {
        public static final int MAX_SCAN_HISTORY_SIZE = 8;
        private final List<ScanEntry> entries = new ArrayList<>();

        public ScanHistory() {}

        public ScanHistory(List<ScanEntry> entries) {
            this.entries.addAll(entries);
        }

        public List<ScanEntry> getEntries() {
            return entries;
        }

        public void addEntry(UUID uuid, String name) {
            entries.removeIf(existing -> existing.uuid().equals(uuid));
            entries.add(new ScanEntry(uuid, name));
            if (entries.size() > MAX_SCAN_HISTORY_SIZE) {
                entries.remove(0);
            }
        }

        public boolean removeEntry(UUID uuid) {
            return entries.removeIf(entry -> entry.uuid().equals(uuid));
        }

        public java.util.Optional<ScanEntry> findEntry(UUID uuid) {
            return entries.stream().filter(entry -> entry.uuid().equals(uuid)).findFirst();
        }

        public static final Codec<ScanHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ScanEntry.CODEC.listOf().fieldOf("entries").forGetter(ScanHistory::getEntries)
        ).apply(instance, ScanHistory::new));
    }
}
