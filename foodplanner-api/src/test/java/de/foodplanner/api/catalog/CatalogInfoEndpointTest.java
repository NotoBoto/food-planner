package de.foodplanner.api.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

/** Endpunktvertrag von {@code GET /api/v1/catalog/info} (§5.1). Jeder Test rollt seine Daten zurück. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class CatalogInfoEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void emptyCatalogReturnsZeroAndNull() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/info"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"productCount\":0,\"lastScrapedAt\":null}", JsonCompareMode.STRICT));
    }

    @Test
    void filledCatalogCountsOnlyProductsWithCurrentPrice() throws Exception {
        long a = product("1001");
        price(a, "2026-11-02T08:00:00Z", false);
        price(a, "2026-11-02T10:00:00Z", true);
        long b = product("1002");
        price(b, "2026-11-02T09:00:00+02:00", true); // = 07:00Z
        long onlyOld = product("1003");
        price(onlyOld, "2026-11-02T12:00:00Z", false); // jünger, aber abgelöst
        product("1004"); // ohne Preis

        mockMvc.perform(get("/api/v1/catalog/info"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"productCount\":2,\"lastScrapedAt\":\"2026-11-02T10:00:00Z\"}", JsonCompareMode.STRICT));
    }

    private long product(String aldiSku) {
        return jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        VALUES (:sku, 'Test ' || :sku, 500, 'G', 'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .query(Long.class)
                .single();
    }

    private void price(long productId, String scrapedAt, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, 199, 'aldi-sued.de', CAST(:scrapedAt AS timestamptz), :current)""")
                .param("productId", productId)
                .param("scrapedAt", scrapedAt)
                .param("current", current)
                .update();
    }
}
