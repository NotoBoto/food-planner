package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
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

    /** catalogInfo zählt Zeilen des Joins; nur dieser Index garantiert eine Zeile je Produkt. */
    @Test
    void secondCurrentPriceForSameProductIsRejected() {
        long product = product("1001");
        price(product, "2026-11-02T10:00:00Z", true);

        assertThatThrownBy(() -> price(product, "2026-11-02T11:00:00Z", true))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("price_one_current_per_product");
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
