package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.application.port.PriceHistoryEntry;
import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.domain.product.ProductKey;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;
import de.foodplanner.infrastructure.TestCatalog;

/**
 * {@link JdbcProductRepository#detail(long)} und {@link JdbcProductRepository#details} gegen PostgreSQL: kein Filter
 * auf einen aktuellen Preis (§5.1), Preisverlauf, Links, Schlüsselabgleich (§4.2, §5.4) und eine feste Zahl an
 * Abfragen.
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import({PostgresTestConfiguration.class, TestCatalog.class, JdbcProductRepository.class})
@Transactional
class JdbcProductDetailTest {

    @Autowired
    JdbcProductRepository repository;

    @Autowired
    TestCatalog catalog;

    @Autowired
    DataSource dataSource;

    @Test
    void detailWithAllFields() {
        // Vier verschiedene Nährwerte, damit eine vertauschte Spalte auffällt; jeder mit Rundung HALF_UP.
        long id = catalog.product("Fettarme Milch").sku("1001").offCode("4008400401211").brand("Marke")
                .category("Molkerei").unit("ML").packageGrams(500).noPrice()
                .nutrition("3.45", "46.05", "1.55", "4.84").insert();
        catalog.price(id, 189, "2026-11-01T10:00:00Z", false);
        catalog.price(id, 199, "2026-11-02T10:00:00Z", true);
        catalog.page(id, "https://www.aldi-sued.de/alt", "2026-11-01T09:00:00Z");
        catalog.page(id, "https://www.aldi-sued.de/neu", "2026-11-02T09:00:00Z");

        assertThat(repository.detail(id)).contains(new ProductDetail(
                id, "aldi:1001", "Fettarme Milch", "Marke", "Molkerei", 500, "ML", 199L, 398L,
                new BigDecimal("3.5"), new BigDecimal("1.6"), new BigDecimal("4.8"), new BigDecimal("46.1"),
                "4008400401211", "Molkerei", "https://www.aldi-sued.de/neu",
                "https://world.openfoodfacts.org/product/4008400401211",
                List.of(entry(199, "2026-11-02T10:00:00Z", true), entry(189, "2026-11-01T10:00:00Z", false))));
    }

    @Test
    void unknownIdIsEmpty() {
        long id = catalog.product("Roggenbrot").insert();

        assertThat(repository.detail(id + 1)).isEmpty();
    }

    @Test
    void productWithoutAnyPriceIsFoundWithoutPrice() {
        long id = catalog.product("Roggenbrot").noPrice().insert();

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isNull();
        assertThat(detail.pricePerKgCents()).isNull();
        assertThat(detail.priceHistory()).isEmpty();
        assertThat(detail.protein100g()).isNull();
    }

    @Test
    void productWithOnlySupersededPricesHasNoPriceButItsHistory() {
        long id = catalog.product("Roggenbrot").noPrice().insert();
        catalog.price(id, 189, "2026-11-01T10:00:00Z", false);
        catalog.price(id, 209, "2026-11-03T10:00:00Z", false);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isNull();
        assertThat(detail.pricePerKgCents()).isNull();
        assertThat(detail.priceHistory()).containsExactly(
                entry(209, "2026-11-03T10:00:00Z", false), entry(189, "2026-11-01T10:00:00Z", false));
    }

    @Test
    void historyIsEmptyWithoutSupersededPrice() {
        long id = catalog.product("Nuss-Nougat-Creme").packageGrams(400).noPrice().insert();
        catalog.price(id, 249, "2026-11-02T10:00:00Z", true);

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.priceCents()).isEqualTo(249L);
        assertThat(detail.pricePerKgCents()).isEqualTo(623L); // 622,5 → HALF_UP
        assertThat(detail.priceHistory()).isEmpty();
    }

    @Test
    void offProductHasNoAldiDepartmentOrUrl() {
        long id = catalog.product("Nuss-Nougat-Creme").noSku().offCode("4008400401211").category("Brotaufstriche")
                .source("OPENFOODFACTS").insert();

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.productKey()).isEqualTo("off:4008400401211");
        assertThat(detail.category()).isEqualTo("Brotaufstriche");
        assertThat(detail.aldiDepartment()).isNull();
        assertThat(detail.aldiUrl()).isNull();
        assertThat(detail.offUrl()).isEqualTo("https://world.openfoodfacts.org/product/4008400401211");
    }

    /** Die Kategorie eines freigegebenen Kandidaten ist keine Aldi-Abteilung, auch mit Artikelnummer und Aldi-Seite. */
    @Test
    void approvedOffProductWithSkuHasNoAldiDepartmentButItsAldiUrl() {
        long id = catalog.product("Nuss-Nougat-Creme").sku("1001").offCode("4008400401211")
                .category("Spreads").source("OPENFOODFACTS").insert();
        catalog.page(id, "https://www.aldi-sued.de/nougat", "2026-11-02T09:00:00Z");

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.productKey()).isEqualTo("aldi:1001");
        assertThat(detail.category()).isEqualTo("Spreads");
        assertThat(detail.aldiDepartment()).isNull();
        assertThat(detail.aldiUrl()).isEqualTo("https://www.aldi-sued.de/nougat");
    }

    @Test
    void seedProductHasNoAldiDepartment() {
        long id = catalog.product("Haferflocken").category("Frühstück").source("SEED").insert();

        assertThat(repository.detail(id).orElseThrow().aldiDepartment()).isNull();
    }

    @Test
    void aldiProductWithoutOffCodeOrPageHasNoLinks() {
        long id = catalog.product("Roggenbrot").sku("1001").insert();

        ProductDetail detail = repository.detail(id).orElseThrow();

        assertThat(detail.offCode()).isNull();
        assertThat(detail.offUrl()).isNull();
        assertThat(detail.aldiUrl()).isNull();
        assertThat(detail.aldiDepartment()).isNull();
    }

    @Test
    void detailsByAldiAndOffKeys() {
        long milk = catalog.product("Milch").sku("1001").insert();
        long spread = catalog.product("Nuss-Nougat-Creme").noSku().offCode("4008400401211").insert();
        catalog.product("Käse").sku("1003").insert();

        assertThat(repository.details(List.of(key("aldi:1001"), key("off:4008400401211"))))
                .extracting(ProductDetail::id)
                .containsExactlyInAnyOrder(milk, spread);
    }

    @Test
    void unknownKeysAreMissing() {
        long milk = catalog.product("Milch").sku("1001").insert();

        assertThat(repository.details(List.of(key("aldi:1001"), key("aldi:9999"), key("off:1234"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void offKeyDoesNotFindProductWithAldiSku() {
        long milk = catalog.product("Milch").sku("1001").offCode("4008400401211").insert();

        assertThat(repository.details(List.of(key("off:4008400401211")))).isEmpty();
        assertThat(repository.details(List.of(key("aldi:1001"), key("off:4008400401211"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void detailsDoNotFilterOnCurrentPriceAndKeepHistoriesApart() {
        long noPrice = catalog.product("Milch").sku("1001").noPrice().insert();
        long superseded = catalog.product("Brot").sku("1002").noPrice().insert();
        catalog.price(superseded, 189, "2026-11-01T10:00:00Z", false);
        long current = catalog.product("Käse").sku("1003").noPrice().insert();
        catalog.price(current, 179, "2026-11-01T10:00:00Z", false);
        catalog.price(current, 199, "2026-11-02T10:00:00Z", true);

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
        long milk = catalog.product("Milch").sku("1001").insert();

        assertThat(repository.details(List.of(key("aldi:1001"), key("aldi:1001"))))
                .extracting(ProductDetail::id)
                .containsExactly(milk);
    }

    @Test
    void noKeysNoProducts() {
        catalog.product("Milch").sku("1001").insert();

        assertThat(repository.details(List.of())).isEmpty();
    }

    /** Entscheidung 11 aus #21: zwei Abfragen, egal wie viele Produkte, mit welcher Historie und welchen Seiten. */
    @Test
    void batchAndDetailRunExactlyTwoStatements() {
        List<ProductKey> keys = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            long id = catalog.product("Produkt " + i).sku("s" + i).noPrice().insert();
            catalog.price(id, 100 + i, "2026-11-01T10:00:00Z", false);
            catalog.price(id, 200 + i, "2026-11-02T10:00:00Z", true);
            catalog.page(id, "https://www.aldi-sued.de/p" + i, "2026-11-02T09:00:00Z");
            keys.add(key("aldi:s" + i));
        }
        long first = repository.details(List.of(keys.getFirst())).getFirst().id();
        AtomicInteger statements = new AtomicInteger();
        JdbcProductRepository counted = new JdbcProductRepository(JdbcClient.create(counting(statements)));

        assertThat(counted.details(keys)).hasSize(30);
        assertThat(statements).hasValue(2);

        statements.set(0);
        assertThat(counted.detail(first)).isPresent();
        assertThat(statements).hasValue(2);

        statements.set(0);
        assertThat(counted.details(Collections.emptyList())).isEmpty();
        assertThat(statements).hasValue(0);
    }

    /**
     * Zählt vorbereitete Anweisungen. Die Verbindung kommt aus der laufenden Testtransaktion, damit die Testdaten
     * sichtbar sind; {@code close()} gibt sie nur an Spring zurück.
     */
    private DataSource counting(AtomicInteger statements) {
        return new DelegatingDataSource(dataSource) {
            @Override
            public Connection getConnection() {
                Connection target = DataSourceUtils.getConnection(dataSource);
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                            if (method.getName().equals("close")) {
                                DataSourceUtils.releaseConnection(target, dataSource);
                                return null;
                            }
                            if (method.getName().equals("prepareStatement")
                                    || method.getName().equals("createStatement")) {
                                statements.incrementAndGet();
                            }
                            try {
                                return method.invoke(target, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
            }
        };
    }

    private static ProductKey key(String raw) {
        return ProductKey.parse(raw).orElseThrow();
    }

    private static PriceHistoryEntry entry(long priceCents, String scrapedAt, boolean current) {
        return new PriceHistoryEntry(priceCents, Instant.parse(scrapedAt), current);
    }
}
