package de.foodplanner.infrastructure.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;

import de.foodplanner.application.port.PriceHistoryEntry;
import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.domain.product.PricePerKg;
import de.foodplanner.domain.product.ProductKey;

/**
 * Baut {@link ProductDetail} aus zwei Abfragen von {@link JdbcProductRepository}: einer Zeile je Produkt
 * ({@link Row}) und den Preisen dieser Produkte. Schlüssel und Kilopreis kommen aus {@code domain}, Nährwerte rundet
 * {@link Nutrients}.
 */
final class ProductDetails {

    /** Produktseite bei Open Food Facts (§5.3). */
    static final String OFF_PRODUCT_URL = "https://world.openfoodfacts.org/product/";

    /** {@code product.source} eines Produkts, das direkt aus einer Aldi-Seite entstanden ist (§4.3). */
    private static final String ALDI_SOURCE = "ALDI";

    /**
     * Erwartete Spalten: {@code id, aldi_sku, off_code, name, brand, category, package_grams, unit, price_cents,
     * protein100, fat100, carbs100, kcal100, source, aldi_url}; {@code price_cents} ist der aktuelle Preis oder
     * {@code NULL}.
     */
    static final RowMapper<Row> ROW = (rs, rowNum) -> new Row(
            rs.getLong("id"),
            rs.getString("aldi_sku"),
            rs.getString("off_code"),
            rs.getString("name"),
            rs.getString("brand"),
            rs.getString("category"),
            rs.getInt("package_grams"),
            rs.getString("unit"),
            rs.getObject("price_cents", Long.class),
            rs.getBigDecimal("protein100"),
            rs.getBigDecimal("fat100"),
            rs.getBigDecimal("carbs100"),
            rs.getBigDecimal("kcal100"),
            rs.getString("source"),
            rs.getString("aldi_url"));

    /** Erwartete Spalten: {@code product_id, price_cents, scraped_at, is_current}. */
    static final RowMapper<Price> PRICE = (rs, rowNum) -> new Price(
            rs.getLong("product_id"),
            new PriceHistoryEntry(
                    rs.getLong("price_cents"),
                    rs.getObject("scraped_at", OffsetDateTime.class).toInstant(),
                    rs.getBoolean("is_current")));

    private ProductDetails() {
    }

    /**
     * @param prices alle Preise des Produkts, neueste zuerst. Der Verlauf ist leer, wenn keiner davon abgelöst ist
     *               (§5.4); sonst enthält er alle, auch den aktuellen.
     */
    static ProductDetail assemble(Row row, List<PriceHistoryEntry> prices) {
        Long pricePerKgCents = row.priceCents() == null ? null : PricePerKg.cents(row.priceCents(), row.packageGrams());
        boolean hasSuperseded = prices.stream().anyMatch(price -> !price.isCurrent());
        return new ProductDetail(
                row.id(),
                ProductKey.of(row.aldiSku(), row.offCode()).value(),
                row.name(),
                row.brand(),
                row.category(),
                row.packageGrams(),
                row.unit(),
                row.priceCents(),
                pricePerKgCents,
                Nutrients.round(row.protein100()),
                Nutrients.round(row.fat100()),
                Nutrients.round(row.carbs100()),
                Nutrients.round(row.kcal100()),
                row.offCode(),
                // Nur bei source = ALDI stammt category aus der BreadcrumbList der Aldi-Seite (§4.3, §6.1) und ist die
                // Abteilung. Ein freigegebenes Open-Food-Facts-Produkt trägt die Kategorie des Kandidaten, auch wenn es
                // aus einer Aldi-Zeile stammt.
                ALDI_SOURCE.equals(row.source()) ? row.category() : null,
                row.aldiUrl(),
                row.offCode() == null ? null : OFF_PRODUCT_URL + row.offCode(),
                hasSuperseded ? List.copyOf(prices) : List.of());
    }

    /** Ein Produkt mit aktuellem Preis und Aldi-URL, noch ohne Preisverlauf. */
    record Row(
            long id,
            @Nullable String aldiSku,
            @Nullable String offCode,
            String name,
            @Nullable String brand,
            @Nullable String category,
            int packageGrams,
            String unit,
            @Nullable Long priceCents,
            @Nullable BigDecimal protein100,
            @Nullable BigDecimal fat100,
            @Nullable BigDecimal carbs100,
            @Nullable BigDecimal kcal100,
            String source,
            @Nullable String aldiUrl) {
    }

    /** Ein Preis mit dem Produkt, zu dem er gehört. */
    record Price(long productId, PriceHistoryEntry entry) {
    }
}
