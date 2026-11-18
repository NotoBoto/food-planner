package de.foodplanner.api.shoppinglist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import de.foodplanner.application.shoppinglist.summarise.ShoppingListSummary;
import de.foodplanner.domain.shoppinglist.Nutrition;
import de.foodplanner.domain.shoppinglist.ShoppingLine;
import de.foodplanner.domain.shoppinglist.ShoppingTotals;

/**
 * Antwort von {@code POST /api/v1/shopping-list/summary}, Feld für Feld nach §5.2. Hier, an der API-Grenze, werden
 * die Nährwerte auf eine Nachkommastelle gerundet ({@code HALF_UP}), an genau dieser einen Stelle (§4.4): Summen und
 * Zielfortschritt sind vorher aus den ungerundeten Werten gerechnet.
 *
 * @param goalProgress       {@code null}, wenn die Anfrage keine Ziele hat
 * @param missingProductKeys Schlüssel, wie gesendet, ohne Produkt im Katalog
 */
record ShoppingListSummaryResponse(
        List<Line> lines,
        Totals totals,
        @Nullable GoalProgress goalProgress,
        List<String> missingProductKeys) {

    static ShoppingListSummaryResponse from(ShoppingListSummary summary) {
        de.foodplanner.domain.shoppinglist.GoalProgress progress = summary.goalProgress();
        return new ShoppingListSummaryResponse(
                summary.lines().stream().map(Line::from).toList(),
                Totals.from(summary.totals()),
                progress == null ? null : new GoalProgress(progress.proteinPct(), progress.fatPct(),
                        progress.carbsPct(), progress.kcalPct(), progress.budgetPct()),
                summary.missingProductKeys());
    }

    /** Die eine Rundung der Antwort: {@code HALF_UP} auf eine Nachkommastelle. */
    private static BigDecimal round(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Eine Zeile. Nährwerte absolut ({@code protein} …) und pro 100 g bzw. 100 ml ({@code protein100g} …); alle
     * {@code null}, wenn das Produkt keine hat.
     */
    record Line(
            String productKey,
            String name,
            int quantity,
            long unitPriceCents,
            long lineTotalCents,
            @Nullable BigDecimal protein,
            @Nullable BigDecimal fat,
            @Nullable BigDecimal carbs,
            @Nullable BigDecimal kcal,
            @Nullable BigDecimal protein100g,
            @Nullable BigDecimal fat100g,
            @Nullable BigDecimal carbs100g,
            @Nullable BigDecimal kcal100g,
            String unit,
            long priceAtAddCents,
            Instant addedAt,
            long priceChangeCents) {

        static Line from(ShoppingListSummary.Line summaryLine) {
            ShoppingLine line = summaryLine.line();
            Nutrition nutrition = line.nutrition();
            Nutrition per100g = summaryLine.per100g();
            return new Line(
                    summaryLine.productKey(),
                    summaryLine.name(),
                    line.quantity(),
                    line.unitPriceCents(),
                    line.lineTotalCents(),
                    nutrition == null ? null : round(nutrition.protein()),
                    nutrition == null ? null : round(nutrition.fat()),
                    nutrition == null ? null : round(nutrition.carbs()),
                    nutrition == null ? null : round(nutrition.kcal()),
                    per100g == null ? null : round(per100g.protein()),
                    per100g == null ? null : round(per100g.fat()),
                    per100g == null ? null : round(per100g.carbs()),
                    per100g == null ? null : round(per100g.kcal()),
                    summaryLine.unit(),
                    line.priceAtAddCents(),
                    summaryLine.addedAt(),
                    line.priceChangeCents());
        }
    }

    /** Summen über alle Zeilen; Nährwerte nur über Zeilen, deren Produkt welche hat. */
    record Totals(
            long priceCents,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal carbs,
            BigDecimal kcal,
            long priceDifferenceCents) {

        static Totals from(ShoppingTotals totals) {
            Nutrition nutrition = totals.nutrition();
            return new Totals(
                    totals.priceCents(),
                    round(nutrition.protein()),
                    round(nutrition.fat()),
                    round(nutrition.carbs()),
                    round(nutrition.kcal()),
                    totals.priceDifferenceCents());
        }
    }

    /** Fortschritt in ganzen Prozent; {@code null} je Ziel, das nicht gesetzt ist. */
    record GoalProgress(
            @Nullable Long proteinPct,
            @Nullable Long fatPct,
            @Nullable Long carbsPct,
            @Nullable Long kcalPct,
            @Nullable Long budgetPct) {
    }
}
