package de.foodplanner.infrastructure.product;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.domain.product.PricePerKg;
import de.foodplanner.domain.product.ProductKey;

/**
 * Baut aus einer Zeile von {@link JdbcProductSearch} ein {@link ProductSummary} (§5.1). Schlüssel und Kilopreis kommen
 * aus {@code domain}; Nährwerte rundet {@link Nutrients}.
 *
 * <p>Erwartete Spalten: {@code id, aldi_sku, off_code, name, brand, category, package_grams, unit, price_cents,
 * protein100, fat100, carbs100, kcal100}.
 */
final class ProductSummaryRowMapper implements RowMapper<ProductSummary> {

    static final ProductSummaryRowMapper INSTANCE = new ProductSummaryRowMapper();

    private ProductSummaryRowMapper() {
    }

    @Override
    public ProductSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        long priceCents = rs.getLong("price_cents");
        int packageGrams = rs.getInt("package_grams");
        return new ProductSummary(
                rs.getLong("id"),
                ProductKey.of(rs.getString("aldi_sku"), rs.getString("off_code")).value(),
                rs.getString("name"),
                rs.getString("brand"),
                rs.getString("category"),
                packageGrams,
                rs.getString("unit"),
                priceCents,
                PricePerKg.cents(priceCents, packageGrams),
                Nutrients.round(rs.getBigDecimal("protein100")),
                Nutrients.round(rs.getBigDecimal("fat100")),
                Nutrients.round(rs.getBigDecimal("carbs100")),
                Nutrients.round(rs.getBigDecimal("kcal100")));
    }
}
