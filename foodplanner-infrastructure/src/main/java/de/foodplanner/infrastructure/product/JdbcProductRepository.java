package de.foodplanner.infrastructure.product;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import de.foodplanner.application.port.CatalogInfo;
import de.foodplanner.application.port.ProductRepository;

/** {@link ProductRepository} über {@link JdbcClient} mit handgeschriebenem SQL (§4.1). */
@Repository
class JdbcProductRepository implements ProductRepository {

    private final JdbcClient jdbc;

    JdbcProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CatalogInfo catalogInfo() {
        // count(*) zählt jedes Produkt einmal: höchstens ein aktueller Preis je Produkt (price_one_current_per_product).
        // Auf einer leeren Menge liefern count/max genau eine Zeile mit 0 und NULL.
        return jdbc.sql("SELECT count(*) AS product_count, max(cp.scraped_at) AS last_scraped_at FROM "
                        + VisibleProducts.FROM)
                .query((rs, rowNum) -> {
                    OffsetDateTime lastScrapedAt = rs.getObject("last_scraped_at", OffsetDateTime.class);
                    return new CatalogInfo(rs.getLong("product_count"),
                            lastScrapedAt == null ? null : lastScrapedAt.toInstant());
                })
                .single();
    }

    @Override
    public List<String> categories() {
        return jdbc.sql("SELECT DISTINCT p.category FROM " + VisibleProducts.FROM
                        + " WHERE p.category IS NOT NULL ORDER BY p.category")
                .query(String.class)
                .list();
    }
}
