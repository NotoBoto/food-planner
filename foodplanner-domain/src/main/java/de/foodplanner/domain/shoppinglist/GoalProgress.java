package de.foodplanner.domain.shoppinglist;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fortschritt zu den Zielen in ganzen Prozent, {@code HALF_UP} gerundet (§5.2). Ein Wert über 100 bedeutet, dass das
 * Ziel überschritten ist; die Anzeige kappt ihn. Ein nicht gesetztes Ziel ergibt {@code null}.
 *
 * @param proteinPct Eiweiß in Prozent des Ziels
 * @param fatPct     Fett in Prozent des Ziels
 * @param carbsPct   Kohlenhydrate in Prozent des Ziels
 * @param kcalPct    Energie in Prozent des Ziels
 * @param budgetPct  Gesamtpreis in Prozent des Budgets
 */
public record GoalProgress(Long proteinPct, Long fatPct, Long carbsPct, Long kcalPct, Long budgetPct) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Rechnet den Fortschritt aus den ungerundeten Summen. Das Budget bleibt im Preispfad ganzzahlig wie
     * {@code PricePerKg}: {@code priceCents × 100 / budgetCents}, der Bruch mit 2 erweitert und um den halben Nenner
     * verschoben. Bei {@code priceCents} bis 200 × 1000 × 100 000 Cent bleibt {@code 200 × priceCents} weit unter
     * {@link Long#MAX_VALUE}; darüber endet die Rechnung als {@link ArithmeticException}.
     *
     * @param goals jedes gesetzte Ziel mindestens 1
     */
    public static GoalProgress of(ShoppingTotals totals, Goals goals) {
        Nutrition nutrition = totals.nutrition();
        return new GoalProgress(
                percent(nutrition.protein(), goals.protein()),
                percent(nutrition.fat(), goals.fat()),
                percent(nutrition.carbs(), goals.carbs()),
                percent(nutrition.kcal(), goals.kcal()),
                goals.budgetCents() == null ? null : budgetPercent(totals.priceCents(), goals.budgetCents()));
    }

    private static Long percent(BigDecimal total, Long goal) {
        if (goal == null) {
            return null;
        }
        return total.multiply(HUNDRED).divide(BigDecimal.valueOf(goal), 0, RoundingMode.HALF_UP).longValueExact();
    }

    private static long budgetPercent(long priceCents, long budgetCents) {
        return Math.floorDiv(Math.addExact(Math.multiplyExact(200, priceCents), budgetCents), 2 * budgetCents);
    }
}
