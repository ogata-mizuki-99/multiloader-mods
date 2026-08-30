package com.ogatamizuki.guide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record GuideAccess(String kind, Identifier bookId, Identifier themeId) {
    public static final String KIND_CODEX = "codex";
    public static final String KIND_MANUAL = "manual";

    public static final Codec<GuideAccess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("kind").forGetter(GuideAccess::kind),
            Identifier.CODEC.optionalFieldOf("book").forGetter(access -> Optional.ofNullable(access.bookId)),
            Identifier.CODEC.optionalFieldOf("theme", GuideTheme.BOOK_ID).forGetter(GuideAccess::themeId)
    ).apply(instance, (kind, book, theme) -> new GuideAccess(kind, book.orElse(null), theme)));

    public static GuideAccess codex(Identifier themeId) {
        return new GuideAccess(KIND_CODEX, null, themeId != null ? themeId : GuideTheme.BOOK_ID);
    }

    public static GuideAccess manual(Identifier bookId, Identifier themeId) {
        return new GuideAccess(KIND_MANUAL, bookId, themeId != null ? themeId : GuideTheme.BOOK_ID);
    }
}
