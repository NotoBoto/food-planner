package de.foodplanner.domain.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ShoppingLineTest {

    /** Vier verschiedene Werte, damit eine vertauschte Komponente auffällt. */
    private static final Nutrition MILK = new Nutrition(
            new BigDecimal("3.45"), new BigDecimal("1.55"), new BigDecimal("4.84"), new BigDecimal("46.05"));

    @Test
    void lineTotalIsQuantityTimesUnitPrice() {
        assertThat(new ShoppingLine(3, 129, 129, 1000, null).lineTotalCents()).isEqualTo(387);
    }

    @Test
    void priceChangeAndDifference() {
        ShoppingLine dearer = new ShoppingLine(2, 139, 129, 1000, null);
        assertThat(dearer.priceChangeCents()).isEqualTo(10);
        assertThat(dearer.priceDifferenceCents()).isEqualTo(20);

        ShoppingLine cheaper = new ShoppingLine(2, 119, 129, 1000, null);
        assertThat(cheaper.priceChangeCents()).isEqualTo(-10);
        assertThat(cheaper.priceDifferenceCents()).isEqualTo(-20);
    }

    @Test
    void nutritionIsQuantityTimesGramsTimesPer100gOver100() {
        Nutrition nutrition = new ShoppingLine(3, 129, 129, 1000, MILK).nutrition();

        assertThat(nutrition.protein()).isEqualByComparingTo("103.5");
        assertThat(nutrition.fat()).isEqualByComparingTo("46.5");
        assertThat(nutrition.carbs()).isEqualByComparingTo("145.2");
        assertThat(nutrition.kcal()).isEqualByComparingTo("1381.5");
    }

    @Test
    void nutritionIsNotRounded() {
        Nutrition nutrition = new ShoppingLine(1, 129, 129, 250, MILK).nutrition();

        assertThat(nutrition.protein()).isEqualByComparingTo("8.625");
    }

    @Test
    void noNutritionStaysNull() {
        assertThat(new ShoppingLine(3, 129, 129, 1000, null).nutrition()).isNull();
    }

    @Test
    void upperBoundsDoNotOverflow() {
        ShoppingLine line = new ShoppingLine(1000, 100_000, 1, 50_000, MILK);

        assertThat(line.lineTotalCents()).isEqualTo(100_000_000);
        assertThat(line.priceDifferenceCents()).isEqualTo(99_999_000);
        assertThat(line.nutrition().kcal()).isEqualByComparingTo("23025000");
    }

    @Test
    void overflowThrows() {
        ShoppingLine line = new ShoppingLine(1000, Long.MAX_VALUE / 10, 1, 500, null);

        assertThatThrownBy(line::lineTotalCents).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(line::priceDifferenceCents).isInstanceOf(ArithmeticException.class);
    }
}
