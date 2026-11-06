package de.foodplanner.infrastructure.aldi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.foodplanner.domain.RawProduct;
import de.foodplanner.domain.ScrapedRowValidator;

class AldiProductParserTest {

    private static final String PRODUCT_URL =
            "https://www.aldi-sued.de/produkt/nusskati-nuss-nougat-creme-400-g-000000000000549987";

    private final AldiProductParser parser = new AldiProductParser();

    private static String fixture(String name) throws IOException {
        try (var in = AldiProductParserTest.class.getResourceAsStream("/fixtures/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void readsAllFieldsFromSavedPage() throws Exception {
        RawProduct product = parser.parse(fixture("aldi_product.html"), "https://fallback").orElseThrow();

        assertThat(product.name()).isEqualTo("NUSSKATI Nuss-Nougat-Creme 400 g");
        assertThat(product.priceCents()).isEqualTo(199L);
        assertThat(product.url()).isEqualTo(PRODUCT_URL);
        assertThat(product.categoryPath()).isEqualTo(List.of("Startseite", "Produkte", "Fairtrade-Produkte"));
        assertThat(product.imageUrl()).contains("NussNougatCreme");
        assertThat(product.aldiSku()).isEqualTo("000000000000549987");
    }

    @Test
    void takesExactMacrosNotSubItems() throws Exception {
        var nutrition = parser.parse(fixture("aldi_product.html"), "x").orElseThrow().nutrition();

        assertThat(nutrition).isNotNull();
        assertThat(nutrition.kcal()).isEqualByComparingTo("553");
        assertThat(nutrition.fat()).isEqualByComparingTo("34.9");
        assertThat(nutrition.carbs()).isEqualByComparingTo("51.2");
        assertThat(nutrition.protein()).isEqualByComparingTo("6.5");
    }

    @Test
    void reversedRowOrderGivesSameResult() throws Exception {
        var normal = parser.parse(fixture("aldi_product.html"), "x").orElseThrow();
        var reversed = parser.parse(fixture("aldi_product_reversed.html"), "x").orElseThrow();

        assertThat(reversed.nutrition()).isEqualTo(normal.nutrition());
    }

    @Test
    void missingMacroMeansNoNutritionAtAll() throws Exception {
        String html = fixture("aldi_product.html").replace("Eiweiß", "Eiweiss");

        assertThat(parser.parse(html, "x").orElseThrow().nutrition()).isNull();
    }

    @Test
    void wrongBasisMeansNoNutrition() throws Exception {
        String html = fixture("aldi_product.html").replace("pro 100 g", "pro Portion");

        assertThat(parser.parse(html, "x").orElseThrow().nutrition()).isNull();
    }

    @Test
    void unparsableValueMeansNoNutrition() throws Exception {
        String html = fixture("aldi_product.html").replace("34,9 g", "34,9abc");

        assertThat(parser.parse(html, "x").orElseThrow().nutrition()).isNull();
    }

    @Test
    void pageWithoutProductGivesEmpty() {
        assertThat(parser.parse("<html></html>", "x")).isEmpty();
    }

    @Test
    void endToEndWithValidatorMarksNotFood() throws Exception {
        RawProduct product = parser.parse(fixture("aldi_product.html"), "x").orElseThrow();

        var food = new ScrapedRowValidator(Set.of("Haushalt")).validate(product).orElseThrow();
        var nonFood = new ScrapedRowValidator(Set.of("Fairtrade-Produkte")).validate(product).orElseThrow();

        assertThat(food.notFood()).isFalse();
        assertThat(food.nutrition()).isNotNull(); // 6,5 + 34,9 + 51,2 = 92,6 <= 105
        assertThat(nonFood.notFood()).isTrue();
    }
}
