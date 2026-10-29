package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.sql.Timestamp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import(PostgresTestConfiguration.class)
@Transactional
class FlywayMigrationTest {

    private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-10-29T10:00:00Z"));

    @Autowired
    JdbcClient jdbc;

    @Test
    void migrationsRunThrough() {
        assertThat(jdbc.sql("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")
                .query(String.class).list())
                .contains("1");
        assertThat(jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE NOT success")
                .query(Long.class).single())
                .isZero();
    }

    @Test
    void createsTheFiveCoreTables() {
        assertThat(jdbc.sql("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
                        """)
                .query(String.class).list())
                .containsExactlyInAnyOrder("product", "price", "scraped_item", "off_candidate", "scraped_page");
    }

    @Test
    void productHasNoSearchColumnsYet() {
        assertThat(jdbc.sql("""
                        SELECT column_name FROM information_schema.columns
                        WHERE table_name = 'product' AND column_name LIKE 'search%'
                        """)
                .query(String.class).list())
                .isEmpty();
    }

    @Test
    void moneyIsBigintAndNutritionIsNumeric() {
        assertThat(columnType("price", "price_cents")).isEqualTo("bigint");
        assertThat(columnType("scraped_item", "price_cents")).isEqualTo("bigint");
        assertThat(columnType("product", "protein100")).isEqualTo("numeric");
        assertThat(columnType("off_candidate", "kcal100")).isEqualTo("numeric");
    }

    @Test
    void productNeedsAtLeastOneKey() {
        assertThatThrownBy(() -> insertProduct(null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void productKeysAreUniqueButOptional() {
        insertProduct("4000000000001", null);
        insertProduct(null, "1001");

        assertThatThrownBy(() -> insertProduct("4000000000001", "1002"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Nach einem Fehler bricht PostgreSQL die Transaktion ab, daher prüft jeder Test genau eine Verletzung.

    @Test
    void unitRejectsUnknownValues() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        VALUES ('1', 'Milch', 1000, 'L', 'ALDI')
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sourceRejectsUnknownValues() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        VALUES ('1', 'Milch', 1000, 'ML', 'LIDL')
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchStateRejectsUnknownValues() {
        assertThatThrownBy(() -> insertScrapedItem("UNKNOWN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void candidateStatusRejectsUnknownValues() {
        long itemId = insertScrapedItem("SEARCHED");

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO off_candidate (scraped_item_id, off_code, confidence, rank, status, decided_at)
                        VALUES (:item, '4000000000001', 0.8, 1, 'MAYBE', :now)
                        """).param("item", itemId).param("now", NOW).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nutritionIsAllFourOrNone() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source, kcal100, protein100)
                        VALUES ('1', 'Milch', 1000, 'ML', 'ALDI', 64, 3.4)
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyOneCurrentPricePerProduct() {
        long productId = insertProduct(null, "1001");
        insertPrice(productId, false);
        insertPrice(productId, true);

        assertThatThrownBy(() -> insertPrice(productId, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void priceNeedsAnExistingProduct() {
        assertThatThrownBy(() -> insertPrice(-1, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void candidateNeedsAnExistingScrapedItem() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO off_candidate (scraped_item_id, off_code, confidence, rank)
                        VALUES (-1, '4000000000001', 0.8, 1)
                        """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void scrapedPageProductMustExistWhenSet() {
        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO scraped_page (url, fetched_at, product_id) VALUES ('https://a', :now, -1)
                        """).param("now", NOW).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void scrapedPageUrlIsUniqueAndProductIsOptional() {
        String sql = "INSERT INTO scraped_page (url, fetched_at) VALUES ('https://a', :now)";
        jdbc.sql(sql).param("now", NOW).update();

        assertThatThrownBy(() -> jdbc.sql(sql).param("now", NOW).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void decidedCandidatesCarryADecisionTime() {
        long itemId = insertScrapedItem("SEARCHED");

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO off_candidate (scraped_item_id, off_code, confidence, rank, status)
                        VALUES (:item, '4000000000001', 0.8, 1, 'APPROVED')
                        """).param("item", itemId).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pendingCandidatesCarryNoDecisionTime() {
        long itemId = insertScrapedItem("SEARCHED");

        assertThatThrownBy(() -> jdbc.sql("""
                        INSERT INTO off_candidate (scraped_item_id, off_code, confidence, rank, status, decided_at)
                        VALUES (:item, '4000000000001', 0.8, 1, 'PENDING', :now)
                        """).param("item", itemId).param("now", NOW).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyOneApprovedCandidatePerScrapedItem() {
        long itemId = insertScrapedItem("SEARCHED");
        String sql = """
                INSERT INTO off_candidate (scraped_item_id, off_code, confidence, rank, status, decided_at)
                VALUES (:item, :offCode, 0.8, 1, 'APPROVED', :now)
                """;
        jdbc.sql(sql).param("item", itemId).param("offCode", "4000000000001").param("now", NOW).update();

        assertThatThrownBy(() -> jdbc.sql(sql)
                        .param("item", itemId).param("offCode", "4000000000002").param("now", NOW).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String columnType(String table, String column) {
        return jdbc.sql("""
                        SELECT data_type FROM information_schema.columns
                        WHERE table_name = :table AND column_name = :column
                        """)
                .param("table", table)
                .param("column", column)
                .query(String.class).single();
    }

    private long insertProduct(String offCode, String aldiSku) {
        return jdbc.sql("""
                        INSERT INTO product (off_code, aldi_sku, name, package_grams, unit, source)
                        VALUES (:offCode, :aldiSku, 'Milch', 1000, 'ML', 'ALDI')
                        RETURNING id
                        """)
                .param("offCode", offCode)
                .param("aldiSku", aldiSku)
                .query(Long.class).single();
    }

    private void insertPrice(long productId, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:product, 99, 'ALDI', :now, :current)
                        """)
                .param("product", productId)
                .param("now", NOW)
                .param("current", current)
                .update();
    }

    private long insertScrapedItem(String searchState) {
        return jdbc.sql("""
                        INSERT INTO scraped_item (raw_name, price_cents, url, scraped_at, search_state)
                        VALUES ('Milch 1 l', 99, 'https://a', :now, :state)
                        RETURNING id
                        """)
                .param("now", NOW)
                .param("state", searchState)
                .query(Long.class).single();
    }
}
