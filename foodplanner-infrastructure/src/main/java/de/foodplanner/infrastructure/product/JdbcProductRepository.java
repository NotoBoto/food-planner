package de.foodplanner.infrastructure.product;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import de.foodplanner.application.port.CatalogInfo;
import de.foodplanner.application.port.PriceHistoryEntry;
import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.port.ProductRepository;
import de.foodplanner.domain.product.ProductKey;

/**
 * {@link ProductRepository} über {@link JdbcClient} mit handgeschriebenem SQL (§4.1).
 *
 * <p>Detail und Sammelabruf nutzen {@link VisibleProducts} bewusst nicht: Sie filtern nicht auf einen aktuellen Preis
 * (§5.1). Beide laufen mit genau zwei Abfragen, unabhängig von der Zahl der Produkte: eine für die Produkte, eine für
 * deren Preise.
 */
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
        // Deutsche Sortierung unabhängig von der Datenbank-Collation: unter einer C-Locale stünde „Äpfel“ hinter „Zucker“.
        // Bei DISTINCT muss die Sortierung im SELECT stehen; de-x-icu ist deterministisch, DISTINCT bleibt also gleich.
        return jdbc.sql("SELECT DISTINCT p.category COLLATE \"de-x-icu\" AS category FROM " + VisibleProducts.FROM
                        + " WHERE p.category IS NOT NULL ORDER BY category")
                .query(String.class)
                .list();
    }

    /**
     * Ein Produkt mit aktuellem Preis (oder {@code NULL}) und der URL der zuletzt abgerufenen Aldi-Seite, die an das
     * Produkt gebunden ist (§6.1). Der Unterabfrage hilft {@code scraped_page_product_id_idx}.
     */
    private static final String DETAIL_SELECT = """
            SELECT p.id, p.aldi_sku, p.off_code, p.name, p.brand, p.category, p.package_grams, p.unit,
                   cp.price_cents, p.protein100, p.fat100, p.carbs100, p.kcal100, p.source,
                   (SELECT sp.url FROM scraped_page sp WHERE sp.product_id = p.id
                     ORDER BY sp.fetched_at DESC, sp.id DESC LIMIT 1) AS aldi_url
            FROM product p
            LEFT JOIN price cp ON cp.product_id = p.id AND cp.is_current
            """;

    @Override
    public Optional<ProductDetail> detail(long id) {
        List<ProductDetails.Row> rows = jdbc.sql(DETAIL_SELECT + "WHERE p.id = ?")
                .param(id)
                .query(ProductDetails.ROW)
                .list();
        return withPriceHistory(rows).stream().findFirst();
    }

    @Override
    public List<ProductDetail> details(Collection<ProductKey> keys) {
        String[] aldiSkus = keys.stream().flatMap(key -> key.aldiSku().stream()).distinct().toArray(String[]::new);
        String[] offCodes = keys.stream().flatMap(key -> key.offCode().stream()).distinct().toArray(String[]::new);
        if (aldiSkus.length == 0 && offCodes.length == 0) {
            return List.of();
        }
        // off:<code> trifft nur Produkte ohne Artikelnummer: Mit Artikelnummer lautet der Schlüssel aldi:<sku> (§4.2).
        List<ProductDetails.Row> rows = jdbc.sql(DETAIL_SELECT
                        + "WHERE p.aldi_sku = ANY(?) OR (p.aldi_sku IS NULL AND p.off_code = ANY(?))")
                .param(aldiSkus)
                .param(offCodes)
                .query(ProductDetails.ROW)
                .list();
        return withPriceHistory(rows);
    }

    /** Lädt die Preise aller Zeilen in einer Abfrage, neueste zuerst, und baut daraus die Details. */
    private List<ProductDetail> withPriceHistory(List<ProductDetails.Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Long[] ids = rows.stream().map(ProductDetails.Row::id).toArray(Long[]::new);
        Map<Long, List<PriceHistoryEntry>> prices = jdbc.sql("""
                        SELECT product_id, price_cents, scraped_at, is_current FROM price
                        WHERE product_id = ANY(?)
                        ORDER BY scraped_at DESC, id DESC""")
                .param(ids)
                .query(ProductDetails.PRICE)
                .list()
                .stream()
                // toList() behält je Produkt die Reihenfolge der Abfrage bei: neueste zuerst.
                .collect(Collectors.groupingBy(ProductDetails.Price::productId,
                        Collectors.mapping(ProductDetails.Price::entry, Collectors.toList())));
        return rows.stream()
                .map(row -> ProductDetails.assemble(row, prices.getOrDefault(row.id(), List.of())))
                .toList();
    }
}
