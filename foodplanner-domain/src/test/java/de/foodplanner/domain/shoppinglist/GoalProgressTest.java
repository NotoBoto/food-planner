package de.foodplanner.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GoalProgressTest {

    private static ShoppingTotals totals(long priceCents, String protein, String fat, String carbs, String kcal) {
        return new ShoppingTotals(priceCents,
                new Nutrition(new BigDecimal(protein), new BigDecimal(fat), new BigDecimal(carbs),
                        new BigDecimal(kcal)),
                0);
    }

    @Test
    void percentPerGoal() {
        GoalProgress progress = GoalProgress.of(totals(2000, "60", "35", "200", "1500"),
                new Goals(120L, 70L, 250L, 2000L, 4000L));

        assertThat(progress).isEqualTo(new GoalProgress(50L, 50L, 80L, 75L, 50L));
    }

    @Test
    void nutrientPercentRoundsHalfUp() {
        assertThat(GoalProgress.of(totals(0, "0", "0", "0", "2005"), kcal(2000)).kcalPct()).isEqualTo(100);
        assertThat(GoalProgress.of(totals(0, "0", "0", "0", "2010"), kcal(2000)).kcalPct()).isEqualTo(101);
        assertThat(GoalProgress.of(totals(0, "0", "0", "0", "2009.99"), kcal(2000)).kcalPct()).isEqualTo(100);
    }

    @Test
    void budgetPercentRoundsHalfUp() {
        assertThat(GoalProgress.of(totals(2010, "0", "0", "0", "0"), budget(4000)).budgetPct()).isEqualTo(50);
        assertThat(GoalProgress.of(totals(1, "0", "0", "0", "0"), budget(200)).budgetPct()).isEqualTo(1);
        assertThat(GoalProgress.of(totals(1, "0", "0", "0", "0"), budget(201)).budgetPct()).isZero();
    }

    @Test
    void canExceed100() {
        assertThat(GoalProgress.of(totals(5000, "0", "0", "0", "0"), budget(4000)).budgetPct()).isEqualTo(125);
        assertThat(GoalProgress.of(totals(0, "0", "0", "0", "3000"), kcal(2000)).kcalPct()).isEqualTo(150);
    }

    @Test
    void unsetGoalGivesNullPct() {
        GoalProgress progress = GoalProgress.of(totals(2000, "60", "35", "200", "1500"),
                new Goals(120L, null, null, null, 4000L));

        assertThat(progress).isEqualTo(new GoalProgress(50L, null, null, null, 50L));
    }

    @Test
    void zeroTotalsGiveZeroPct() {
        GoalProgress progress = GoalProgress.of(ShoppingTotals.of(java.util.List.of()),
                new Goals(120L, 70L, 250L, 2000L, 4000L));

        assertThat(progress).isEqualTo(new GoalProgress(0L, 0L, 0L, 0L, 0L));
    }

    @Test
    void largestBudgetDoesNotOverflow() {
        long budget = Integer.MAX_VALUE;

        assertThat(GoalProgress.of(totals(200L * 1000 * 100_000, "0", "0", "0", "0"), budget(budget)).budgetPct())
                .isEqualTo(931);
    }

    @Test
    void budgetOverflowThrows() {
        assertThatThrownBy(() -> GoalProgress.of(totals(Long.MAX_VALUE / 100, "0", "0", "0", "0"), budget(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    private static Goals kcal(long kcal) {
        return new Goals(null, null, null, kcal, null);
    }

    private static Goals budget(long budgetCents) {
        return new Goals(null, null, null, null, budgetCents);
    }
}
