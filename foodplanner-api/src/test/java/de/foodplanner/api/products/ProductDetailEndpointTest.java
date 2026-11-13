package de.foodplanner.api.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * Endpunktverträge von {@code GET /api/v1/products/{id}} und {@code POST /api/v1/products/batch} (§5.1, §5.4). Jeder
 * Test rollt seine Daten zurück. Fehlerfälle prüfen nur Status und {@code errors}, nicht den ganzen Text: Das Format
 * vervollständigt #25.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class ProductDetailEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbc;

    @Test
    void detailContract() throws Exception {
        long id = product("1001", "4008400401211", "Fettarme Milch 1,5 %", "ML", 500, "3.45");
        price(id, 189, "2026-11-01T10:00:00Z", false);
        price(id, 199, "2026-11-02T10:00:00Z", true);
        jdbc.sql("INSERT INTO scraped_page (url, fetched_at, product_id) VALUES (?, now(), ?)")
                .params("https://www.aldi-sued.de/de/p.fettarme-milch.1001.html", id)
                .update();

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"productKey":"aldi:1001","name":"Fettarme Milch 1,5 %%","brand":null,
                         "category":"Molkerei","packageGrams":500,"unit":"ML",
                         "priceCents":199,"pricePerKgCents":398,
                         "protein100g":3.5,"fat100g":3.5,"carbs100g":3.5,"kcal100g":3.5,
                         "offCode":"4008400401211","aldiDepartment":"Molkerei",
                         "aldiUrl":"https://www.aldi-sued.de/de/p.fettarme-milch.1001.html",
                         "offUrl":"https://world.openfoodfacts.org/product/4008400401211",
                         "priceHistory":[
                           {"priceCents":199,"scrapedAt":"2026-11-02T10:00:00Z","isCurrent":true},
                           {"priceCents":189,"scrapedAt":"2026-11-01T10:00:00Z","isCurrent":false}]}"""
                        .formatted(id), JsonCompareMode.STRICT));
    }

    @Test
    void unknownIdIs404() throws Exception {
        long id = product("1001", null, "Roggenbrot", "G", 500, null);

        mockMvc.perform(get("/api/v1/products/{id}", id + 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailDoesNotFilterOnCurrentPrice() throws Exception {
        long noPrice = product("1001", null, "Roggenbrot", "G", 500, null);
        long superseded = product("1002", null, "Vollkornbrot", "G", 500, null);
        price(superseded, 189, "2026-11-01T10:00:00Z", false);

        mockMvc.perform(get("/api/v1/products/{id}", noPrice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value((Object) null))
                .andExpect(jsonPath("$.pricePerKgCents").value((Object) null))
                .andExpect(jsonPath("$.priceHistory").isEmpty());
        mockMvc.perform(get("/api/v1/products/{id}", superseded))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value((Object) null))
                .andExpect(jsonPath("$.priceHistory[0].priceCents").value(189))
                .andExpect(jsonPath("$.priceHistory[0].isCurrent").value(false));
    }

    @Test
    void historyIsEmptyWithoutSupersededPrice() throws Exception {
        long id = product("1001", null, "Roggenbrot", "G", 750, null);
        price(id, 189, "2026-11-02T10:00:00Z", true);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(189))
                .andExpect(jsonPath("$.pricePerKgCents").value(252))
                .andExpect(jsonPath("$.priceHistory").isEmpty());
    }

    @Test
    void literPriceForMl() throws Exception {
        long id = product("1001", null, "Olivenöl", "ML", 750, null);
        price(id, 599, "2026-11-02T10:00:00Z", true);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(jsonPath("$.unit").value("ML"))
                .andExpect(jsonPath("$.pricePerKgCents").value(799)); // 798,67 → HALF_UP
    }

    @Test
    void batchWithAldiAndOffKeys() throws Exception {
        long milk = product("1001", null, "Fettarme Milch", "ML", 1000, null);
        long spread = product(null, "4008400401211", "Nuss-Nougat-Creme", "G", 400, null);
        price(milk, 109, "2026-11-02T10:00:00Z", true);

        batch("off:4008400401211", "aldi:1001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(spread))
                .andExpect(jsonPath("$[0].productKey").value("off:4008400401211"))
                .andExpect(jsonPath("$[0].priceCents").value((Object) null))
                .andExpect(jsonPath("$[1].id").value(milk))
                .andExpect(jsonPath("$[1].productKey").value("aldi:1001"))
                .andExpect(jsonPath("$[1].priceCents").value(109))
                .andExpect(jsonPath("$[1].priceHistory").isArray());
    }

    @Test
    void unknownAndMalformedKeysAreMissingWithoutError() throws Exception {
        long milk = product("1001", null, "Fettarme Milch", "ML", 1000, null);

        batch("aldi:9999", "aldi:1001", "off:1234", "foo", "aldi:")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(milk));
        mockMvc.perform(post("/api/v1/products/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keys\":[null,\"aldi:1001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void offKeyOfProductWithAldiSkuIsUnknown() throws Exception {
        product("1001", "4008400401211", "Fettarme Milch", "ML", 1000, null);

        batch("off:4008400401211")
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void duplicatesOnceInOrderOfFirstAppearance() throws Exception {
        long milk = product("1001", null, "Fettarme Milch", "ML", 1000, null);
        long bread = product("1002", null, "Roggenbrot", "G", 500, null);

        batch("aldi:1002", "aldi:1001", "aldi:1002")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(bread))
                .andExpect(jsonPath("$[1].id").value(milk));
    }

    @Test
    void emptyKeysIsEmptyArray() throws Exception {
        batch().andExpect(status().isOk()).andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void missingKeysIs400() throws Exception {
        mockMvc.perform(post("/api/v1/products/batch").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.keys").exists());
    }

    @Test
    void exactly200KeysIsOk() throws Exception {
        product("1001", null, "Fettarme Milch", "ML", 1000, null);

        batch(Collections.nCopies(200, "aldi:1001").toArray(String[]::new))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void moreThan200KeysIs400() throws Exception {
        batch(Collections.nCopies(201, "aldi:1001").toArray(String[]::new))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.keys").exists());
    }

    private ResultActions batch(String... keys) throws Exception {
        String body = List.of(keys).stream().map(key -> "\"" + key + "\"")
                .collect(Collectors.joining(",", "{\"keys\":[", "]}"));
        return mockMvc.perform(post("/api/v1/products/batch").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Legt ein Produkt an; {@code nutrient} gilt für alle vier Nährwerte, {@code null} für keinen. */
    private long product(@Nullable String aldiSku, @Nullable String offCode, String name, String unit,
            int packageGrams, @Nullable String nutrient) {
        return jdbc.sql("""
                        INSERT INTO product (aldi_sku, off_code, name, category, package_grams, unit,
                                             kcal100, protein100, fat100, carbs100, source)
                        VALUES (CAST(:sku AS text), CAST(:off AS text), :name, 'Molkerei', :grams, :unit,
                                CAST(:n AS numeric), CAST(:n AS numeric), CAST(:n AS numeric), CAST(:n AS numeric),
                                'ALDI')
                        RETURNING id""")
                .param("sku", aldiSku)
                .param("off", offCode)
                .param("name", name)
                .param("grams", packageGrams)
                .param("unit", unit)
                .param("n", nutrient)
                .query(Long.class)
                .single();
    }

    private void price(long productId, long priceCents, String scrapedAt, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, :price, 'aldi-sued.de', CAST(:scrapedAt AS timestamptz), :current)""")
                .param("productId", productId)
                .param("price", priceCents)
                .param("scrapedAt", scrapedAt)
                .param("current", current)
                .update();
    }
}
