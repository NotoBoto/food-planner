package de.foodplanner.infrastructure;

import java.math.BigDecimal;
import java.sql.Types;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Testdaten für den Katalog: legt Produkte, Preise und gebundene Aldi-Seiten per SQL an. Gemeinsam für die
 * Datenbanktests in {@code infrastructure} und die Endpunkttests in {@code api}; per {@code @Import} einbinden.
 */
public class TestCatalog {

    private final JdbcClient jdbc;
    private int skuCounter;

    public TestCatalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Ein Produkt mit eigener Artikelnummer, aktuellem Preis von 199 Cent (sichtbar), 500 g, ohne Nährwerte. */
    public ProductBuilder product(String name) {
        return new ProductBuilder(name, "sku-" + ++skuCounter);
    }

    /** Ein Preis zum Zeitpunkt {@code now()}. */
    public void price(long productId, long priceCents, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, :priceCents, 'aldi-sued.de', now(), :current)""")
                .param("productId", productId)
                .param("priceCents", priceCents)
                .param("current", current)
                .update();
    }

    /** Ein Preis zu einem festen Zeitpunkt, etwa {@code 2026-11-02T10:00:00Z}. */
    public void price(long productId, long priceCents, String scrapedAt, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, :priceCents, 'aldi-sued.de', CAST(:scrapedAt AS timestamptz), :current)""")
                .param("productId", productId)
                .param("priceCents", priceCents)
                .param("scrapedAt", scrapedAt)
                .param("current", current)
                .update();
    }

    /** Eine an das Produkt gebundene Aldi-Seite (§6.1). */
    public void page(long productId, String url, String fetchedAt) {
        jdbc.sql("""
                        INSERT INTO scraped_page (url, fetched_at, product_id)
                        VALUES (:url, CAST(:fetchedAt AS timestamptz), :productId)""")
                .param("url", url)
                .param("fetchedAt", fetchedAt)
                .param("productId", productId)
                .update();
    }

    /** Baut ein Produkt auf; {@link #insert()} schreibt es samt aktuellem Preis, falls einer gesetzt ist. */
    public final class ProductBuilder {

        private final String name;
        private @Nullable String sku;
        private @Nullable String offCode;
        private @Nullable String brand;
        private @Nullable String category;
        private int packageGrams = 500;
        private String unit = "G";
        private String source = "ALDI";
        private @Nullable Long priceCents = 199L;
        private @Nullable BigDecimal protein;
        private @Nullable BigDecimal kcal;
        private @Nullable BigDecimal fat;
        private @Nullable BigDecimal carbs;

        private ProductBuilder(String name, String sku) {
            this.name = name;
            this.sku = sku;
        }

        public ProductBuilder sku(@Nullable String value) {
            sku = value;
            return this;
        }

        public ProductBuilder noSku() {
            sku = null;
            return this;
        }

        public ProductBuilder offCode(@Nullable String value) {
            offCode = value;
            return this;
        }

        public ProductBuilder brand(String value) {
            brand = value;
            return this;
        }

        public ProductBuilder category(@Nullable String value) {
            category = value;
            return this;
        }

        public ProductBuilder packageGrams(int value) {
            packageGrams = value;
            return this;
        }

        public ProductBuilder unit(String value) {
            unit = value;
            return this;
        }

        /** {@code ALDI} (Standard), {@code OPENFOODFACTS} oder {@code SEED}. */
        public ProductBuilder source(String value) {
            source = value;
            return this;
        }

        /** Aktueller Preis zum Zeitpunkt {@code now()}. */
        public ProductBuilder price(long cents) {
            priceCents = cents;
            return this;
        }

        /** Ohne aktuellen Preis; Preise mit eigenem Zeitpunkt legt {@link TestCatalog#price} an. */
        public ProductBuilder noPrice() {
            priceCents = null;
            return this;
        }

        /** Protein und kcal; Fett und Kohlenhydrate erhalten Platzhalter, denn es gilt „alle vier oder keiner“. */
        public ProductBuilder nutrition(String protein100, String kcal100) {
            return nutrition(protein100, kcal100, "1.0", "1.0");
        }

        public ProductBuilder nutrition(String protein100, String kcal100, String fat100, String carbs100) {
            protein = new BigDecimal(protein100);
            kcal = new BigDecimal(kcal100);
            fat = new BigDecimal(fat100);
            carbs = new BigDecimal(carbs100);
            return this;
        }

        public long insert() {
            long id = jdbc.sql("""
                            INSERT INTO product (aldi_sku, off_code, name, brand, category, package_grams, unit,
                                                 kcal100, protein100, fat100, carbs100, source)
                            VALUES (:sku, :offCode, :name, :brand, :category, :packageGrams, :unit,
                                    :kcal, :protein, :fat, :carbs, :source)
                            RETURNING id""")
                    .param("sku", sku, Types.VARCHAR)
                    .param("offCode", offCode, Types.VARCHAR)
                    .param("name", name)
                    .param("brand", brand, Types.VARCHAR)
                    .param("category", category, Types.VARCHAR)
                    .param("packageGrams", packageGrams)
                    .param("unit", unit)
                    .param("kcal", kcal, Types.NUMERIC)
                    .param("protein", protein, Types.NUMERIC)
                    .param("fat", fat, Types.NUMERIC)
                    .param("carbs", carbs, Types.NUMERIC)
                    .param("source", source)
                    .query(Long.class)
                    .single();
            if (priceCents != null) {
                TestCatalog.this.price(id, priceCents, true);
            }
            return id;
        }
    }
}
