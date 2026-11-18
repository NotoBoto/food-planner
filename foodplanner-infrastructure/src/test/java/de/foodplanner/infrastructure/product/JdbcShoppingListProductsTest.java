package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.application.port.ShoppingListProduct;
import de.foodplanner.domain.product.ProductKey;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;
import de.foodplanner.infrastructure.TestCatalog;

/**
 * {@link JdbcProductRepository#shoppingListProducts} gegen PostgreSQL: nur Produkte mit aktuellem Preis (§5.2),
 * ungerundete Nährwerte (§4.4), Schlüsselabgleich wie im Sammelabruf und genau eine Abfrage.
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import({PostgresTestConfiguration.class, TestCatalog.class, JdbcProductRepository.class})
@Transactional
class JdbcShoppingListProductsTest {

    @Autowired
    JdbcProductRepository repository;

    @Autowired
    TestCatalog catalog;

    @Autowired
    DataSource dataSource;

    @Test
    void fieldsAndUnroundedNutrients() {
        // Vier verschiedene Nährwerte mit zwei Nachkommastellen: gerundet wären es 3.5, 46.1, 1.6 und 4.8.
        long id = catalog.product("Fettarme Milch").sku("1001").unit("ML").packageGrams(500).noPrice()
                .nutrition("3.45", "46.05", "1.55", "4.84").insert();
        catalog.price(id, 189, "2026-11-01T10:00:00Z", false);
        catalog.price(id, 199, "2026-11-02T10:00:00Z", true);

        assertThat(repository.shoppingListProducts(List.of(key("aldi:1001")))).singleElement().satisfies(p -> {
            assertThat(p.productKey()).isEqualTo("aldi:1001");
            assertThat(p.name()).isEqualTo("Fettarme Milch");
            assertThat(p.packageGrams()).isEqualTo(500);
            assertThat(p.unit()).isEqualTo("ML");
            assertThat(p.priceCents()).isEqualTo(199);
            assertThat(p.protein100()).isEqualByComparingTo("3.45");
            assertThat(p.kcal100()).isEqualByComparingTo("46.05");
            assertThat(p.fat100()).isEqualByComparingTo("1.55");
            assertThat(p.carbs100()).isEqualByComparingTo("4.84");
        });
    }

    @Test
    void withoutNutritionAllNull() {
        catalog.product("Roggenbrot").sku("1002").insert();

        assertThat(repository.shoppingListProducts(List.of(key("aldi:1002")))).singleElement().satisfies(p -> {
            assertThat(p.protein100()).isNull();
            assertThat(p.fat100()).isNull();
            assertThat(p.carbs100()).isNull();
            assertThat(p.kcal100()).isNull();
        });
    }

    @Test
    void productWithoutCurrentPriceIsAbsent() {
        catalog.product("Roggenbrot").sku("1002").noPrice().insert();
        long superseded = catalog.product("Vollkornbrot").sku("1003").noPrice().insert();
        catalog.price(superseded, 189, "2026-11-01T10:00:00Z", false);

        assertThat(repository.shoppingListProducts(List.of(key("aldi:1002"), key("aldi:1003")))).isEmpty();
    }

    @Test
    void keyMatchingLikeBatch() {
        catalog.product("Nuss-Nougat-Creme").noSku().offCode("4008400401211").insert();
        catalog.product("Fettarme Milch").sku("1001").offCode("4008400401228").insert();

        assertThat(repository.shoppingListProducts(List.of(
                key("off:4008400401211"), key("off:4008400401228"), key("aldi:9999"))))
                .extracting(ShoppingListProduct::productKey)
                .containsExactly("off:4008400401211");
    }

    @Test
    void duplicateKeysYieldEachProductOnce() {
        catalog.product("Fettarme Milch").sku("1001").insert();

        assertThat(repository.shoppingListProducts(List.of(key("aldi:1001"), key("aldi:1001")))).hasSize(1);
    }

    /** Entscheidung 11: eine Abfrage für die ganze Liste, keine ohne Schlüssel. */
    @Test
    void exactlyOneStatement() {
        List<ProductKey> keys = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            long id = catalog.product("Produkt " + i).sku("s" + i).noPrice().insert();
            catalog.price(id, 100 + i, "2026-11-01T10:00:00Z", false);
            catalog.price(id, 200 + i, "2026-11-02T10:00:00Z", true);
            keys.add(key("aldi:s" + i));
        }
        AtomicInteger statements = new AtomicInteger();
        JdbcProductRepository counted =
                new JdbcProductRepository(JdbcClient.create(CountingDataSource.of(dataSource, statements)));

        assertThat(counted.shoppingListProducts(keys)).hasSize(30);
        assertThat(statements).hasValue(1);

        statements.set(0);
        assertThat(counted.shoppingListProducts(List.of())).isEmpty();
        assertThat(statements).hasValue(0);
    }

    private static ProductKey key(String raw) {
        return ProductKey.parse(raw).orElseThrow();
    }
}
