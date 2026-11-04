package de.foodplanner.api.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

/** Endpunktvertrag von {@code GET /api/v1/products/search} (§5.1). Jeder Test rollt seine Daten zurück. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class ProductSearchEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void summaryContract() throws Exception {
        long id = product("1001", null, "Fettarme Milch 1,5 %", "ML", 500, 199);

        mockMvc.perform(get("/api/v1/products/search").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[{"id":%d,"productKey":"aldi:1001","name":"Fettarme Milch 1,5 %%",
                          "brand":null,"category":"Molkerei","packageGrams":500,"unit":"ML",
                          "priceCents":199,"pricePerKgCents":398,
                          "protein100g":null,"fat100g":null,"carbs100g":null,"kcal100g":null}],
                         "page":1,"pageSize":20,"totalCount":1,"totalPages":1}""".formatted(id),
                        JsonCompareMode.STRICT));
    }

    @Test
    void lexicalSearch() throws Exception {
        product("1001", null, "Fettarme Milch 1,5 %", "ML", 1000, 109);
        product("1002", null, "Roggenbrot", "G", 500, 189);

        mockMvc.perform(get("/api/v1/products/search").param("q", "milch fettarm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Fettarme Milch 1,5 %"));
    }

    @Test
    void eanSearch() throws Exception {
        product("1001", "4008400401211", "Nuss-Nougat-Creme", "G", 400, 249);

        mockMvc.perform(get("/api/v1/products/search").param("q", "4008400401211"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].productKey").value("aldi:1001"));
        mockMvc.perform(get("/api/v1/products/search").param("q", "4008400401211").param("category", "Gibtsnicht"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void everySortValueIsAccepted() throws Exception {
        for (String sort : new String[] {"relevance", "price_asc", "price_desc", "protein_desc", "kcal_asc"}) {
            mockMvc.perform(get("/api/v1/products/search").param("sort", sort))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void defaultsArePage1Size20() throws Exception {
        mockMvc.perform(get("/api/v1/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    void emptyCategoryIsNoFilter() throws Exception {
        product("1001", null, "Fettarme Milch", "ML", 1000, 109);
        product("1002", null, "Roggenbrot", "G", 500, 189);

        mockMvc.perform(get("/api/v1/products/search").param("category", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2));
        mockMvc.perform(get("/api/v1/products/search").param("q", "milch").param("category", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void unknownSortIs400() throws Exception {
        // errors.sort kommt mit #25 (Bindungsfehler)
        mockMvc.perform(get("/api/v1/products/search").param("sort", "foo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pageAndPageSizeBounds() throws Exception {
        mockMvc.perform(get("/api/v1/products/search").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").exists());
        mockMvc.perform(get("/api/v1/products/search").param("page", "100001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").exists());
        mockMvc.perform(get("/api/v1/products/search").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pageSize").exists());
        mockMvc.perform(get("/api/v1/products/search").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pageSize").exists());
        mockMvc.perform(get("/api/v1/products/search").param("page", "100000"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/search").param("pageSize", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void minAboveMaxIs400() throws Exception {
        mockMvc.perform(get("/api/v1/products/search")
                        .param("minPriceCents", "500").param("maxPriceCents", "400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minPriceCents").exists());
    }

    @Test
    void qLongerThan200CharactersIs400() throws Exception {
        mockMvc.perform(get("/api/v1/products/search").param("q", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.q").value("darf höchstens 200 Zeichen lang sein"));
        mockMvc.perform(get("/api/v1/products/search").param("q", "a".repeat(200)))
                .andExpect(status().isOk());
    }

    private long product(String sku, String offCode, String name, String unit, int grams, long cents) {
        long id = jdbc.sql("""
                        INSERT INTO product (aldi_sku, off_code, name, category, package_grams, unit, source)
                        VALUES (:sku, :off, :name, 'Molkerei', :grams, :unit, 'ALDI')
                        RETURNING id""")
                .param("sku", sku)
                .param("off", offCode)
                .param("name", name)
                .param("grams", grams)
                .param("unit", unit)
                .query(Long.class)
                .single();
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:id, :cents, 'aldi-sued.de', now(), true)""")
                .param("id", id)
                .param("cents", cents)
                .update();
        return id;
    }
}
