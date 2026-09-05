package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.platform.Services;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuideBookRegistry {
    private static Map<Identifier, GuideBook> books = Map.of();

    private GuideBookRegistry() {}

    public static void setBooks(Map<Identifier, GuideBook> loadedBooks) {
        books = Collections.unmodifiableMap(new LinkedHashMap<>(loadedBooks));
    }

    public static GuideBook getBook(Identifier id) {
        return books.get(id);
    }

    public static List<GuideBook> getAllBooks() {
        List<GuideBook> list = new ArrayList<>(books.values());
        list.sort(Comparator
                .comparingInt(GuideBook::sortPriority)
                .thenComparing(book -> book.id().getNamespace())
                .thenComparing(book -> book.id().getPath()));
        return list;
    }

    public static List<GuideBook> getCodexBooks() {
        return getAllBooks().stream().filter(GuideBookRegistry::isVisibleInCodex).toList();
    }

    public static boolean isVisibleInCodex(GuideBook book) {
        if (!book.codexVisible()) {
            return false;
        }
        return !book.devOnly() || Services.PLATFORM.isDevelopmentEnvironment();
    }

    public static boolean isEmpty() {
        return books.isEmpty();
    }
}
