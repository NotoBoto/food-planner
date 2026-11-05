package de.foodplanner.domain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Zeilenvalidierung und NOT_FOOD-Markierung (§6.1). */
public final class ScrapedRowValidator {

    static final long MIN_PRICE_CENTS = 1;
    static final long MAX_PRICE_CENTS = 100_000;
    static final int MAX_NAME_LENGTH = 300;
    static final int MAX_URL_LENGTH = 1000;

    private final Set<String> nonFoodCategories;

    public ScrapedRowValidator(Set<String> nonFoodCategories) {
        this.nonFoodCategories = nonFoodCategories.stream()
                .map(ScrapedRowValidator::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<ScrapedRow> validate(RawProduct raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.name() == null ? "" : raw.name().strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            return Optional.empty();
        }
        Long price = raw.priceCents();
        if (price == null || price < MIN_PRICE_CENTS || price > MAX_PRICE_CENTS) {
            return Optional.empty();
        }
        String url = raw.url();
        if (url == null || url.isBlank() || url.length() > MAX_URL_LENGTH) {
            return Optional.empty();
        }

        Per100g nutrition = NutritionPlausibility.isPlausible(raw.nutrition()) ? raw.nutrition() : null;

        List<String> path = raw.categoryPath() == null ? List.of() : raw.categoryPath();
        boolean notFood = path.stream().map(ScrapedRowValidator::normalize).anyMatch(nonFoodCategories::contains);
        String category = path.isEmpty() ? null : path.get(path.size() - 1);

        return Optional.of(new ScrapedRow(name, price, url, category, raw.imageUrl(),
                raw.aldiSku(), nutrition, notFood));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
