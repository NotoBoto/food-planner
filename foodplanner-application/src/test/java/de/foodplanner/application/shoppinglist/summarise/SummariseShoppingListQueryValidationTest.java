package de.foodplanner.application.shoppinglist.summarise;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Goals;
import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Item;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SummariseShoppingListQueryValidationTest {

    private static final Instant ADDED = Instant.parse("2026-11-20T10:00:00Z");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Item item(String key) {
        return new Item(key, 2L, 129L, ADDED);
    }

    private static List<Item> items(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> item("aldi:" + i)).toList();
    }

    private Set<ConstraintViolation<SummariseShoppingListQuery>> validate(List<Item> items, Goals goals) {
        return validator.validate(new SummariseShoppingListQuery(items, goals));
    }

    private Set<ConstraintViolation<SummariseShoppingListQuery>> validate(Item item) {
        return validate(List.of(item), null);
    }

    /** Genau eine Verletzung, deren Pfad auf {@code field} endet, wie ihn {@code errors.<field>} zeigt. */
    private static void assertViolation(Set<ConstraintViolation<SummariseShoppingListQuery>> violations, String field,
            String message) {
        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.getPropertyPath().toString()).endsWith(field);
            assertThat(v.getMessage()).isEqualTo(message);
        });
    }

    @Test
    void validQuery() {
        assertThat(validate(items(3), new Goals(120L, 70L, 250L, 2000L, 4000L))).isEmpty();
    }

    @Test
    void emptyItemsValid() {
        assertThat(validate(List.of(), null)).isEmpty();
    }

    @Test
    void partialGoalsValid() {
        assertThat(validate(items(1), new Goals(120L, null, null, null, null))).isEmpty();
        assertThat(validate(items(1), new Goals(null, null, null, null, null))).isEmpty();
    }

    @Test
    void itemsRequired() {
        assertViolation(validate(null, null), "items", "darf nicht leer sein");
    }

    @Test
    void atMost200Items() {
        assertThat(validate(items(200), null)).isEmpty();
        assertViolation(validate(items(201), null), "items", "darf höchstens 200 Einträge enthalten");
    }

    @Test
    void quantityBounds() {
        assertThat(validate(new Item("aldi:1", 1L, 129L, ADDED))).isEmpty();
        assertThat(validate(new Item("aldi:1", 1000L, 129L, ADDED))).isEmpty();
        assertViolation(validate(new Item("aldi:1", 0L, 129L, ADDED)), "quantity", "muss zwischen 1 und 1000 liegen");
        assertViolation(validate(new Item("aldi:1", 1001L, 129L, ADDED)), "quantity",
                "muss zwischen 1 und 1000 liegen");
    }

    @Test
    void priceAtAddBounds() {
        assertThat(validate(new Item("aldi:1", 1L, 1L, ADDED))).isEmpty();
        assertThat(validate(new Item("aldi:1", 1L, 100_000L, ADDED))).isEmpty();
        assertViolation(validate(new Item("aldi:1", 1L, 0L, ADDED)), "priceAtAddCents",
                "muss zwischen 1 und 100000 liegen");
        assertViolation(validate(new Item("aldi:1", 1L, 100_001L, ADDED)), "priceAtAddCents",
                "muss zwischen 1 und 100000 liegen");
    }

    @Test
    void everyItemFieldRequired() {
        assertViolation(validate(new Item(null, 1L, 129L, ADDED)), "productKey", "darf nicht leer sein");
        assertViolation(validate(new Item("aldi:1", null, 129L, ADDED)), "quantity", "darf nicht leer sein");
        assertViolation(validate(new Item("aldi:1", 1L, null, ADDED)), "priceAtAddCents", "darf nicht leer sein");
        assertViolation(validate(new Item("aldi:1", 1L, 129L, null)), "addedAt", "darf nicht leer sein");
    }

    @Test
    void productKeyAtMost64Characters() {
        assertThat(validate(item("aldi:" + "1".repeat(59)))).isEmpty();
        assertViolation(validate(item("aldi:" + "1".repeat(60))), "productKey",
                "darf höchstens 64 Zeichen lang sein");
    }

    @Test
    void malformedKeyIsNoViolation() {
        assertThat(validate(List.of(item("foo"), item("")), null)).isEmpty();
    }

    @Test
    void nullItemRejected() {
        assertViolation(validate(Arrays.asList(item("aldi:1"), null), null), "items",
                "darf keine leeren Einträge enthalten");
    }

    @Test
    void duplicateKeysRejected() {
        List<Item> items = new ArrayList<>(items(3));
        items.add(new Item("aldi:2", 5L, 99L, ADDED));

        assertViolation(validate(items, null), "items", "jeder productKey darf nur einmal vorkommen");
    }

    @Test
    void goalBounds() {
        assertThat(validate(items(1), new Goals(1L, 1L, 1L, 1L, 1L))).isEmpty();
        long max = Integer.MAX_VALUE;
        assertThat(validate(items(1), new Goals(max, max, max, max, max))).isEmpty();

        String message = "muss zwischen 1 und 2147483647 liegen";
        assertViolation(validate(items(1), new Goals(0L, null, null, null, null)), "protein", message);
        assertViolation(validate(items(1), new Goals(null, 0L, null, null, null)), "fat", message);
        assertViolation(validate(items(1), new Goals(null, null, 0L, null, null)), "carbs", message);
        assertViolation(validate(items(1), new Goals(null, null, null, 0L, null)), "kcal", message);
        assertViolation(validate(items(1), new Goals(null, null, null, null, 0L)), "budgetCents", message);
        assertViolation(validate(items(1), new Goals(null, null, null, null, max + 1)), "budgetCents", message);
        assertViolation(validate(items(1), new Goals(max + 1, null, null, null, null)), "protein", message);
    }
}
