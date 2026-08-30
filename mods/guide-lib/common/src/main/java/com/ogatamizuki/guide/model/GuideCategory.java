package com.ogatamizuki.guide.model;

import java.util.List;

public record GuideCategory(
        String id,
        String nameKey,
        List<String> entryIds
) {
}
