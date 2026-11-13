package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.application.port.PriceHistoryEntry;
import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.domain.product.ProductKey;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * {@link JdbcProductRepository#detail(long)} und {@link JdbcProductRepository#details} gegen PostgreSQL: kein Filter
 * auf einen aktuellen Preis (§5.1), Preisverlauf, Links und Schlüsselabgleich (§4.2, §5.4).
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import({PostgresTestConfiguration.class, JdbcProductRepository.class})
@Transactional
class JdbcProductDetailTest {

    @Autowired
    JdbcProductRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Test
    void detailWithAllFields() {
        long id = product("1001", "4008400401211", "Molkerei", "ML", 500, new BigDecimal("3.45"));
        price(id, 189, "2026-11-01T10:00:00Z", false);
        price(id, 199, "2026-11-02T10:00:00Z", true);
        page(id, "https://www.aldi-sued.de/alt", "2026-11-01T09:00:00Z");
        page(id, "https://www.aldi-sued.de/neu", "2026-11-02T09:00:00Z");

        assertThat(repository.detail(id)).contains(new ProductDetail(
                id, "aldi:1001", "Produkt 1001", "Marke", "Molkerei", 500, "ML", 199L, 398L,
                new BigDecimal("3.5"), new BigDecimal("3.5"), new BigDecimal("3.5"), new BigDecimal("3.5"),
                "4008400401211", "Molkerei", "https://www.aldi-sued.de/neu",
                "https://world.openfoodfacts.org/product/4008400401211",
                List.of(entry(199, "2026-11-02T10:00:00Z", true), entry(189, "2026-11-01T10:00:00Z", false))));
    }

    @Test
    void unknownIdIsEmpty() {
        long id = product("1001", null, null, "G", 500, null);

        assertThat(repository.detail(id + 1)).isEmpty();
    }

    @Test
    void productWithoutAnyPriceIsFoundWithoutPrice() {
        long id = product("1001", null, null, "G", 500, null);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isNull();
        assertThat(detail.pricePerKgCents()).isNull();
        assertThat(detail.priceHistory()).isEmpty();
        assertThat(detail.protein100g()).isNull();
    }

    @Test
    void productWithOnlySupersededPricesHasNoPriceButItsHistory() {
        long id = product("1001", null, null, "G", 500, null);
        price(id, 189, "2026-11-01T10:00:00Z", false);
        price(id, 209, "2026-11-03T10:00:00Z", false);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isNull();
        assertThat(detail.pricePerKgCents()).isNull();
        assertThat(detail.priceHistory()).containsExactly(
                entry(209, "2026-11-03T10:00:00Z", false), entry(189, "2026-11-01T10:00:00Z", false));
    }

    @Test
    void historyIsEmptyWithoutSupersededPrice() {
        long id = product("1001", null, null, "G", 400, null);
        price(id, 249, "2026-11-02T10:00:00Z", true);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isEqualTo(249L);
        assertThat(detail.pricePerKgCents()).isEqualTo(623L); // 622,5 → HALF_UP
        assertThat(detail.priceHistory()).isEmpty();
    }

    @Test
    void offProductHasNoAldiDepartmentOrUrl() {
        long id = product(null, "4008400401211", "Brotaufstriche", "G", 400, null);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.productKey()).isEqualTo("off:4008400401211");
        assertThat(detail.category()).isEqualTo("Brotaufstriche");
        assertThat(detail.aldiDepartment()).isNull();
        assertThat(detail.aldiUrl()).isNull();
        assertThat(detail.offUrl()).isEqualTo("https://world.openfoodfacts.org/product/4008400401211");
    }

    @Test
    void aldiProductWithoutOffCodeOrPageHasNoLinks() {
        long id = product("1001", null, null, "G", 500, null);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.offCode()).isNull();
        assertThat(detail.offUrl()).isNull();
        assertThat(detail.aldiUrl()).isNull();
        assertThat(detail.aldiDepartment()).isNull();
    }

    @Test
    void detailsByAldiAndOffKeys() {
        long milk = product("1001", null, null, "G", 500, null);
        long spread = product(null, "4008400401211", null, "G", 400, null);
        product("1003", null, null, "G", 500, null);

        assertThat(repository.details(List.of(key("aldi:1001"), key("off:4008400401211"))))
                .extracting(ProductDetail::id)
                .containsExactlyInAnyOrder(milk, spread);
    }

    @Test
    void unknownKeysAreMissing() {
        long milk = product("1001", null, null, "G", 500, null);

        assertThat(repository.details(List.of(key("aldi:1001"), key("aldi:9999"), key("off:1234"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void offKeyDoesNotFindProductWithAldiSku() {
        long milk = product("1001", "4008400401211", null, "G", 500, null);

        assertThat(repository.details(List.of(key("off:4008400401211")))).isEmpty();
        assertThat(repository.details(List.of(key("aldi:1001"), key("off:4008400401211"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void detailsDoNotFilterOnCurrentPriceAndKeepHistoriesApart() {
        long noPrice = product("1001", null, null, "G", 500, null);
        long superseded = product("1002", null, null, "G", 500, null);
        price(superseded, 189, "2026-11-01T10:00:00Z", false);
        long current = product("1003", null, null, "G", 500, null);
        price(current, 179, "2026-11-01T10:00:00Z", false);
        price(current, 199, "2026-11-02T10:00:00Z", true);

        List<ProductDetail> details = repository.details(List.of(key("aldi:1001"), key("aldi:1002"), key("aldi:1003")));

        assertThat(details).extracting(ProductDetail::id).containsExactlyInAnyOrder(noPrice, superseded, current);
        assertThat(details).filteredOn(d -> d.id() == superseded).singleElement().satisfies(d -> {
            assertThat(d.priceCents()).isNull();
            assertThat(d.priceHistory()).containsExactly(entry(189, "2026-11-01T10:00:00Z", false));
        });
        assertThat(details).filteredOn(d -> d.id() == current).singleElement().satisfies(d -> {
            assertThat(d.priceCents()).isEqualTo(199L);
            assertThat(d.priceHistory()).containsExactly(
                    entry(199, "2026-11-02T10:00:00Z", true), entry(179, "2026-11-01T10:00:00Z", false));
        });
        assertThat(details).filteredOn(d -> d.id() == noPrice).singleElement()
                .satisfies(d -> assertThat(d.priceHistory()).isEmpty());
    }

    @Test
    void duplicateKeysYieldEachProductOnce() {
        long milk = product("1001", null, null, "G", 500, null);

        assertThat(repository.details(List.of(key("aldi:1001"), key("aldi:1001"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void noKeysNoProducts() {
        product("1001", null, null, "G", 500, null);

        assertThat(repository.details(List.of())).isEmpty();
    }

    private static ProductKey key(String raw) {
        return ProductKey.parse(raw).orElseThrow();
    }

    private static PriceHistoryEntry entry(long priceCents, String scrapedAt, boolean current) {
        return new PriceHistoryEntry(priceCents, Instant.parse(scrapedAt), current);
    }

    /** Legt ein Produkt an; {@code nutrient} gilt für alle vier Nährwerte, {@code null} für keinen. */
    private long product(@Nullable String aldiSku, @Nullable String offCode, @Nullable String category, String unit,
            int packageGrams, @Nullable BigDecimal nutrient) {
        return jdbc.sql("""
                        INSERT INTO product (aldi_sku, off_code, name, brand, category, package_grams, unit,
                                             kcal100, protein100, fat100, carbs100, source)
                        VALUES (CAST(:sku AS text), CAST(:off AS text),
                                'Produkt ' || coalesce(CAST(:sku AS text), CAST(:off AS text)), 'Marke', :category,
                                :grams, :unit, CAST(:nutrient AS numeric), CAST(:nutrient AS numeric),
                                CAST(:nutrient AS numeric), CAST(:nutrient AS numeric), 'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .param("off", offCode)
                .param("category", category)
                .param("grams", packageGrams)
                .param("unit", unit)
                .param("nutrient", nutrient)
                .query(Long.class)
                .single();
    }

    private void price(long productId, long priceCents, String scrapedAt, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, :price, 'aldi-sued.de', CAST(:scrapedAt AS timestamptz), :current)""")
                .param("productId", productId)
                .param("price", priceCents)
                .param("scrapedAt", scrapedAt)
                .param("current", current)
                .update();
    }

    private void page(long productId, String url, String fetchedAt) {
        jdbc.sql("""
                        INSERT INTO scraped_page (url, fetched_at, product_id)
                        VALUES (:url, CAST(:fetchedAt AS timestamptz), :productId)""")
                .param("url", url)
                .param("fetchedAt", fetchedAt)
                .param("productId", productId)
                .update();
    }
}
