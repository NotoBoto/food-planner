package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.application.port.CatalogInfo;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * {@link JdbcProductRepository#catalogInfo()} und damit die Sichtbarkeitsregel {@link VisibleProducts} gegen
 * PostgreSQL. Suche (#19), Ausblenden (§10.2) und Auslistung (§10.5) erweitern die Regel; diese Fälle müssen dabei
 * gelten bleiben.
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import({PostgresTestConfiguration.class, JdbcProductRepository.class})
@Transactional
class JdbcProductRepositoryTest {

    @Autowired
    JdbcProductRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Test
    void emptyCatalogHasNoProductsAndNoScrapeTime() {
        assertThat(repository.catalogInfo()).isEqualTo(new CatalogInfo(0, null));
    }

    @Test
    void productWithCurrentPriceIsVisible() {
        long product = product("1001");
        price(product, "2026-11-02T10:00:00Z", true);

        assertThat(repository.catalogInfo())
                .isEqualTo(new CatalogInfo(1, Instant.parse("2026-11-02T10:00:00Z")));
    }

    @Test
    void productWithoutPriceIsNotVisible() {
        product("1001");

        assertThat(repository.catalogInfo()).isEqualTo(new CatalogInfo(0, null));
    }

    @Test
    void productWithOnlySupersededPricesIsNotVisibleAndDoesNotSetScrapeTime() {
        long product = product("1001");
        price(product, "2026-11-02T08:00:00Z", false);
        price(product, "2026-11-02T12:00:00Z", false);

        assertThat(repository.catalogInfo()).isEqualTo(new CatalogInfo(0, null));
    }

    @Test
    void productWithHistoryIsCountedOnceAndUsesItsCurrentPrice() {
        long product = product("1001");
        price(product, "2026-11-02T06:00:00Z", false);
        price(product, "2026-11-02T08:00:00Z", false);
        price(product, "2026-11-02T10:00:00Z", true);

        assertThat(repository.catalogInfo())
                .isEqualTo(new CatalogInfo(1, Instant.parse("2026-11-02T10:00:00Z")));
    }

    @Test
    void lastScrapedAtIsTheNewestCurrentPriceAcrossProducts() {
        price(product("1001"), "2026-11-02T10:00:00Z", true);
        price(product("1002"), "2026-11-02T13:00:00+02:00", true); // = 11:00Z, das jüngste
        long superseded = product("1003");
        price(superseded, "2026-11-02T12:00:00Z", false); // jünger, aber abgelöst
        price(superseded, "2026-11-02T09:00:00Z", true);

        assertThat(repository.catalogInfo())
                .isEqualTo(new CatalogInfo(3, Instant.parse("2026-11-02T11:00:00Z")));
    }

    @Test
    void categoriesAreDistinctSortedAndVisibleOnly() {
        price(product("1001", "Obst"), "2026-11-02T10:00:00Z", true);
        price(product("1002", "Obst"), "2026-11-02T10:00:00Z", true);
        price(product("1003", "Brot"), "2026-11-02T10:00:00Z", true);
        price(product("1004", null), "2026-11-02T10:00:00Z", true);
        price(product("1005", "Käse"), "2026-11-02T10:00:00Z", false); // nur abgelöster Preis
        product("1006", "Fleisch"); // ohne Preis
        price(product("1007", "Zucker"), "2026-11-02T10:00:00Z", true);
        price(product("1008", "Äpfel"), "2026-11-02T10:00:00Z", true); // unter einer C-Locale hinter „Zucker“

        assertThat(repository.categories()).containsExactly("Äpfel", "Brot", "Obst", "Zucker");
    }

    @Test
    void noCategoriesInEmptyCatalog() {
        assertThat(repository.categories()).isEqualTo(List.of());
    }

    private long product(String aldiSku, String category) {
        return jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, category, package_grams, unit, source)
                        VALUES (:sku, 'Test ' || :sku, :category, 500, 'G', 'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .param("category", category)
                .query(Long.class)
                .single();
    }

    private long product(String aldiSku) {
        return jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        VALUES (:sku, 'Test ' || :sku, 500, 'G', 'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .query(Long.class)
                .single();
    }

    private void price(long productId, String scrapedAt, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, 199, 'aldi-sued.de', CAST(:scrapedAt AS timestamptz), :current)""")
                .param("productId", productId)
                .param("scrapedAt", scrapedAt)
                .param("current", current)
                .update();
    }
}
