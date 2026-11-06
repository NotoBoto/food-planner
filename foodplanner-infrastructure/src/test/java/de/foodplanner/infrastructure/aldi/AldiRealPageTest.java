package de.foodplanner.infrastructure.aldi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.foodplanner.domain.Per100g;
import de.foodplanner.domain.RawProduct;

/**
 * Der Aldi-Parser auf echten, unverändert gespeicherten Seiten (§8, §9.3).
 *
 * <p>Die erwarteten Werte sind von Hand von der Seite abgelesen, nicht aus einem Parser-Lauf übernommen.
 * Eine fehlende Fixture lässt den Test scheitern, statt ihn zu überspringen.
 */
class AldiRealPageTest {

    private static final String DIR = "/fixtures/aldi/";

    /** nutrition == null heißt: auf dieser Seite dürfen keine Nährwerte herauskommen. */
    record Expected(String file, String name, long priceCents, String sku, Per100g nutrition) {
        @Override
        public String toString() {
            return file;
        }
    }

    private static Per100g n(String kcal, String protein, String fat, String carbs) {
        return new Per100g(new BigDecimal(kcal), new BigDecimal(protein), new BigDecimal(fat), new BigDecimal(carbs));
    }

    static Stream<Expected> pages() {
        return Stream.of(
                new Expected("nusskati-nuss-nougat-creme.html",
                        "NUSSKATI Nuss-Nougat-Creme 400 g", 199L, "000000000000549987",
                        n("553", "6.5", "34.9", "51.2")),
                new Expected("milch-100ml.html",
                        "MILSANI Frische Vollmilch 3,5 % 1 l", 95L, "000000000330654001",
                        null),
                new Expected("ohne-naehrwerte.html",
                        "MEINE BACKWELT Butter-Laugenecke", 79L, "000000000000187148",
                        null),
                new Expected("non-food.html",
                        "TANDIL Colorwaschmittel 2,025 kg", 469L, "000000000000504879",
                        null)
        );
    }

    private static String load(String file) throws IOException {
        try (InputStream in = AldiRealPageTest.class.getResourceAsStream(DIR + file)) {
            assertThat(in).as("Fixture %s fehlt", file).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pages")
    void parsesRealSavedPage(Expected expected) throws Exception {
        RawProduct product = new AldiProductParser()
                .parse(load(expected.file()), "https://www.aldi-sued.de/")
                .orElseThrow();

        assertThat(product.name()).isEqualTo(expected.name());
        assertThat(product.priceCents()).isEqualTo(expected.priceCents());
        assertThat(product.aldiSku()).isEqualTo(expected.sku());
        assertThat(product.url()).startsWith("https://www.aldi-sued.de/produkt/");
        assertThat(product.imageUrl()).startsWith("https://");
        assertThat(product.categoryPath()).isNotEmpty();

        if (expected.nutrition() == null) {
            assertThat(product.nutrition()).isNull();
        } else {
            assertThat(product.nutrition()).isNotNull();
            assertThat(product.nutrition().kcal()).isEqualByComparingTo(expected.nutrition().kcal());
            assertThat(product.nutrition().protein()).isEqualByComparingTo(expected.nutrition().protein());
            assertThat(product.nutrition().fat()).isEqualByComparingTo(expected.nutrition().fat());
            assertThat(product.nutrition().carbs()).isEqualByComparingTo(expected.nutrition().carbs());
        }
    }
}
