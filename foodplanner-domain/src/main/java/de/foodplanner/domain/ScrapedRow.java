package de.foodplanner.domain;


public record ScrapedRow(
        String rawName,
        long priceCents,
        String url,
        String category,
        String imageUrl,
        String aldiSku,
        Per100g nutrition,
        boolean notFood) {
}
