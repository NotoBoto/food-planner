package de.foodplanner.application.shoppinglist.summarise;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.ProductRepository;
import de.foodplanner.application.port.ShoppingListProduct;
import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Item;
import de.foodplanner.domain.product.ProductKey;
import de.foodplanner.domain.shoppinglist.GoalProgress;
import de.foodplanner.domain.shoppinglist.Goals;
import de.foodplanner.domain.shoppinglist.Nutrition;
import de.foodplanner.domain.shoppinglist.ShoppingLine;
import de.foodplanner.domain.shoppinglist.ShoppingTotals;
import jakarta.validation.Valid;

/**
 * Listenzusammenfassung (§5.2): rechnet aus der Liste der Anfrage Zeilen, Summen, Preisänderung seit dem Hinzufügen
 * und Zielfortschritt. Eine reine Funktion des Request-Bodys ohne Zustand (§4.6); gerechnet wird in {@code domain}.
 *
 * <p>Die Produkte kommen mit einem Port-Aufruf für die ganze Liste. Ein Schlüssel ohne Produkt mit aktuellem Preis,
 * auch ein falsch geformter, landet in {@code missingProductKeys} statt in den Summen; Zeilen und fehlende Schlüssel
 * folgen der Reihenfolge der Anfrage.
 */
@Service
@Validated
public class SummariseShoppingListHandler {

    private final ProductRepository productRepository;

    public SummariseShoppingListHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ShoppingListSummary handle(@Valid SummariseShoppingListQuery query) {
        // Nach der Validierung: items gesetzt, ohne null-Einträge, jede Zeile vollständig.
        List<Item> items = Objects.requireNonNull(query.items()).stream().map(Objects::requireNonNull).toList();
        Map<String, ShoppingListProduct> products = load(items);

        List<ShoppingListSummary.Line> lines = new ArrayList<>();
        List<String> missingProductKeys = new ArrayList<>();
        for (Item item : items) {
            String key = Objects.requireNonNull(item.productKey());
            ShoppingListProduct product = products.get(key);
            if (product == null) {
                missingProductKeys.add(key);
            } else {
                lines.add(line(item, product));
            }
        }
        ShoppingTotals totals = ShoppingTotals.of(lines.stream().map(ShoppingListSummary.Line::line).toList());
        SummariseShoppingListQuery.Goals goals = query.goals();
        GoalProgress goalProgress = goals == null ? null : GoalProgress.of(totals,
                new Goals(goals.protein(), goals.fat(), goals.carbs(), goals.kcal(), goals.budgetCents()));
        return new ShoppingListSummary(List.copyOf(lines), totals, goalProgress, List.copyOf(missingProductKeys));
    }

    /** Ein Port-Aufruf für alle gültigen Schlüssel; ohne einen gibt es keinen Aufruf. */
    private Map<String, ShoppingListProduct> load(List<Item> items) {
        Set<ProductKey> keys = new LinkedHashSet<>();
        for (Item item : items) {
            ProductKey.parse(item.productKey()).ifPresent(keys::add);
        }
        Map<String, ShoppingListProduct> byKey = new HashMap<>();
        if (!keys.isEmpty()) {
            for (ShoppingListProduct product : productRepository.shoppingListProducts(keys)) {
                byKey.put(product.productKey(), product);
            }
        }
        return byKey;
    }

    private static ShoppingListSummary.Line line(Item item, ShoppingListProduct product) {
        ShoppingLine line = new ShoppingLine(Objects.requireNonNull(item.quantity()), product.priceCents(),
                Objects.requireNonNull(item.priceAtAddCents()), product.packageGrams(), per100g(product));
        return new ShoppingListSummary.Line(product.productKey(), product.name(), product.unit(),
                Objects.requireNonNull(item.addedAt()), line);
    }

    /** Die vier Nährwerte, oder {@code null}: Sie sind immer alle gesetzt oder alle leer (§4.4). */
    private static @Nullable Nutrition per100g(ShoppingListProduct product) {
        BigDecimal protein = product.protein100();
        BigDecimal fat = product.fat100();
        BigDecimal carbs = product.carbs100();
        BigDecimal kcal = product.kcal100();
        if (protein == null || fat == null || carbs == null || kcal == null) {
            return null;
        }
        return new Nutrition(protein, fat, carbs, kcal);
    }
}
