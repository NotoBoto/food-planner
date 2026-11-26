package de.foodplanner.domain;

import java.util.List;

public record RawProduct(
        String name,
        Long priceCents,
        String url,
        List<String> categoryPath,
        String imageUrl,
        String aldiSku,
        Per100g nutrition) {
}
