package de.foodplanner.api.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.infrastructure.PostgresTestConfiguration;
import de.foodplanner.infrastructure.TestCatalog;

/**
 * Endpunktverträge von {@code GET /api/v1/products/{id}} und {@code POST /api/v1/products/batch} (§5.1, §5.4). Jeder
 * Test rollt seine Daten zurück. Fehlerfälle prüfen nur Status und {@code errors}, nicht den ganzen Text: Das Format
 * vervollständigt #25.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, TestCatalog.class})
@Transactional
class ProductDetailEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestCatalog catalog;

    @Test
    void detailContract() throws Exception {
        // Vier verschiedene Nährwerte, damit eine vertauschte Spalte auffällt; jeder mit Rundung HALF_UP.
        long id = catalog.product("Fettarme Milch 1,5 %").sku("1001").offCode("4008400401211").category("Molkerei")
                .unit("ML").packageGrams(500).noPrice().nutrition("3.45", "46.05", "1.55", "4.84").insert();
        catalog.price(id, 189, "2026-11-01T10:00:00Z", false);
        catalog.price(id, 199, "2026-11-02T10:00:00Z", true);
        catalog.page(id, "https://www.aldi-sued.de/de/p.fettarme-milch.1001.html", "2026-11-02T09:00:00Z");

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"productKey":"aldi:1001","name":"Fettarme Milch 1,5 %%","brand":null,
                         "category":"Molkerei","packageGrams":500,"unit":"ML",
                         "priceCents":199,"pricePerKgCents":398,
                         "protein100g":3.5,"fat100g":1.6,"carbs100g":4.8,"kcal100g":46.1,
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
        long id = catalog.product("Roggenbrot").insert();

        mockMvc.perform(get("/api/v1/products/{id}", id + 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOnBatchIsWrongMethod() throws Exception {
        mockMvc.perform(get("/api/v1/products/batch"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void detailDoesNotFilterOnCurrentPrice() throws Exception {
        long noPrice = catalog.product("Roggenbrot").noPrice().insert();
        long superseded = catalog.product("Vollkornbrot").noPrice().insert();
        catalog.price(superseded, 189, "2026-11-01T10:00:00Z", false);

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
        long id = catalog.product("Roggenbrot").packageGrams(750).price(189).insert();

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(189))
                .andExpect(jsonPath("$.pricePerKgCents").value(252))
                .andExpect(jsonPath("$.priceHistory").isEmpty());
    }

    @Test
    void literPriceForMl() throws Exception {
        long id = catalog.product("Olivenöl").unit("ML").packageGrams(750).price(599).insert();

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(jsonPath("$.unit").value("ML"))
                .andExpect(jsonPath("$.pricePerKgCents").value(799)); // 798,67 → HALF_UP
    }

    @Test
    void batchWithAldiAndOffKeys() throws Exception {
        long milk = catalog.product("Fettarme Milch").sku("1001").price(109).insert();
        long spread = catalog.product("Nuss-Nougat-Creme").noSku().offCode("4008400401211").noPrice().insert();

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
        long milk = catalog.product("Fettarme Milch").sku("1001").insert();

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
        catalog.product("Fettarme Milch").sku("1001").offCode("4008400401211").insert();

        batch("off:4008400401211")
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void duplicatesOnceInOrderOfFirstAppearance() throws Exception {
        long milk = catalog.product("Fettarme Milch").sku("1001").insert();
        long bread = catalog.product("Roggenbrot").sku("1002").insert();

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
        catalog.product("Fettarme Milch").sku("1001").insert();

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

    @Test
    void keyLongerThan64CharactersIs400() throws Exception {
        batch("aldi:1001", "aldi:" + "1".repeat(60))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.keys").exists());
    }

    private ResultActions batch(String... keys) throws Exception {
        String body = List.of(keys).stream().map(key -> "\"" + key + "\"")
                .collect(Collectors.joining(",", "{\"keys\":[", "]}"));
        return mockMvc.perform(post("/api/v1/products/batch").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
