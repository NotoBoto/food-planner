package de.foodplanner.api.shoppinglist;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery;

/**
 * JSON-Body von {@code POST /api/v1/shopping-list/summary} (§5.2). Der API-Vertrag bleibt so vom Query-Record der
 * Anwendung getrennt; geprüft wird erst im Handler (§3.2). Jedes Feld darf hier fehlen: Ein fehlendes Feld wird
 * {@code null} und vom {@code @Validated}-Handler als {@code errors.<feld>} gemeldet.
 *
 * @param items Zeilen der Liste (§4.6)
 * @param goals Ziele, oder {@code null}
 */
record ShoppingListSummaryRequest(@Nullable List<@Nullable Item> items, @Nullable Goals goals) {

    SummariseShoppingListQuery toQuery() {
        List<SummariseShoppingListQuery.@Nullable Item> queryItems = items == null ? null
                : items.stream().map(item -> item == null ? null : item.toQuery()).toList();
        return new SummariseShoppingListQuery(queryItems, goals == null ? null : goals.toQuery());
    }

    /** Eine Zeile: {@code { productKey, quantity, priceAtAddCents, addedAt }} (§5.2). */
    record Item(
            @Nullable String productKey,
            @Nullable Integer quantity,
            @Nullable Long priceAtAddCents,
            @Nullable Instant addedAt) {

        SummariseShoppingListQuery.Item toQuery() {
            return new SummariseShoppingListQuery.Item(productKey, quantity, priceAtAddCents, addedAt);
        }
    }

    /** Ziele: {@code { protein, fat, carbs, kcal, budgetCents }}, jedes einzeln optional (§7.2). */
    record Goals(
            @Nullable Long protein,
            @Nullable Long fat,
            @Nullable Long carbs,
            @Nullable Long kcal,
            @Nullable Long budgetCents) {

        SummariseShoppingListQuery.Goals toQuery() {
            return new SummariseShoppingListQuery.Goals(protein, fat, carbs, kcal, budgetCents);
        }
    }
}
