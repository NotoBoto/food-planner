package de.foodplanner.infrastructure.aldi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AldiRealPageTest {

    @Test
    void parsesRealSavedNusskatiPage() throws Exception {
        try (var in = AldiRealPageTest.class.getResourceAsStream("/fixtures/aldi_nusskati.html")) {
            assumeTrue(in != null, "aldi_nusskati.html nicht vorhanden");
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            var product = new AldiProductParser().parse(html, "https://www.aldi-sued.de/").orElseThrow();

            assertThat(product.name()).isEqualTo("NUSSKATI Nuss-Nougat-Creme 400 g");
            assertThat(product.priceCents()).isEqualTo(199L);
            assertThat(product.aldiSku()).isEqualTo("000000000000549987");
            assertThat(product.nutrition().kcal()).isEqualByComparingTo("553");
            assertThat(product.nutrition().fat()).isEqualByComparingTo("34.9");
            assertThat(product.nutrition().carbs()).isEqualByComparingTo("51.2");
            assertThat(product.nutrition().protein()).isEqualByComparingTo("6.5");
        }
    }
}
