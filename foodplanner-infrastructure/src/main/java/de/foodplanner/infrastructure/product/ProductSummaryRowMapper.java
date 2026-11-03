package de.foodplanner.infrastructure.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;

import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.domain.product.PricePerKg;
import de.foodplanner.domain.product.ProductKey;

/**
 * Baut aus einer Zeile von {@link JdbcProductSearch} ein {@link ProductSummary} (§5.1). Schlüssel und Kilopreis kommen
 * aus {@code domain}; Nährwerte werden auf eine Nachkommastelle gerundet ({@code HALF_UP}), {@code NULL} bleibt
 * {@code null}.
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
                nutrient(rs.getBigDecimal("protein100")),
                nutrient(rs.getBigDecimal("fat100")),
                nutrient(rs.getBigDecimal("carbs100")),
                nutrient(rs.getBigDecimal("kcal100")));
    }

    private static @Nullable BigDecimal nutrient(@Nullable BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }
}
