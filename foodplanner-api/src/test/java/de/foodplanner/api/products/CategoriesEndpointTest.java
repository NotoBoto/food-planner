package de.foodplanner.api.products;

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

/** Endpunktvertrag von {@code GET /api/v1/categories} (§5.1). Jeder Test rollt seine Daten zurück. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class CategoriesEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void emptyCatalogReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void filledCatalogReturnsDistinctSortedCategories() throws Exception {
        visibleProduct("1001", "Obst");
        visibleProduct("1002", "Obst");
        visibleProduct("1003", "Brot");
        visibleProduct("1004", null);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Brot\",\"Obst\"]", JsonCompareMode.STRICT));
    }

    private void visibleProduct(String aldiSku, String category) {
        long id = jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, category, package_grams, unit, source)
                        VALUES (:sku, 'Test ' || :sku, :category, 500, 'G', 'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .param("category", category)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, 199, 'aldi-sued.de', now(), true)""")
                .param("productId", id)
                .update();
    }
}
