package de.foodplanner.api.shoppinglist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
 * Endpunktvertrag von {@code POST /api/v1/shopping-list/summary} (§5.2). Jeder Test rollt seine Daten zurück.
 * Fehlerfälle prüfen nur Status und {@code errors.<feld>}, nicht den ganzen Text: Das Format vervollständigt #25.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, TestCatalog.class})
@Transactional
class ShoppingListSummaryEndpointTest {

    private static final String ADDED = "2026-11-20T10:00:00Z";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestCatalog catalog;

    @Test
    void contract() throws Exception {
        // Vier verschiedene Nährwerte, damit eine vertauschte Spalte auffällt; pro 100 ml auf eine Stelle gerundet.
        catalog.product("Fettarme Milch").sku("1001").unit("ML").packageGrams(1000).price(139)
                .nutrition("3.45", "46.05", "1.55", "4.84").insert();
        catalog.product("Roggenbrot").sku("1002").packageGrams(750).price(189).insert();

        summary("""
                {"items":[
                  {"productKey":"aldi:1001","quantity":3,"priceAtAddCents":129,"addedAt":"%1$s"},
                  {"productKey":"aldi:9999","quantity":1,"priceAtAddCents":99,"addedAt":"%1$s"},
                  {"productKey":"aldi:1002","quantity":1,"priceAtAddCents":199,"addedAt":"%1$s"}],
                 "goals":{"protein":120,"fat":70,"carbs":250,"kcal":2000,"budgetCents":4000}}""".formatted(ADDED))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"lines":[
                          {"productKey":"aldi:1001","name":"Fettarme Milch","quantity":3,
                           "unitPriceCents":139,"lineTotalCents":417,
                           "protein":103.5,"fat":46.5,"carbs":145.2,"kcal":1381.5,
                           "protein100g":3.5,"fat100g":1.6,"carbs100g":4.8,"kcal100g":46.1,"unit":"ML",
                           "priceAtAddCents":129,"addedAt":"2026-11-20T10:00:00Z","priceChangeCents":10},
                          {"productKey":"aldi:1002","name":"Roggenbrot","quantity":1,
                           "unitPriceCents":189,"lineTotalCents":189,
                           "protein":null,"fat":null,"carbs":null,"kcal":null,
                           "protein100g":null,"fat100g":null,"carbs100g":null,"kcal100g":null,"unit":"G",
                           "priceAtAddCents":199,"addedAt":"2026-11-20T10:00:00Z","priceChangeCents":-10}],
                         "totals":{"priceCents":606,"protein":103.5,"fat":46.5,"carbs":145.2,"kcal":1381.5,
                                   "priceDifferenceCents":20},
                         "goalProgress":{"proteinPct":86,"fatPct":66,"carbsPct":58,"kcalPct":69,"budgetPct":15},
                         "missingProductKeys":["aldi:9999"]}""", JsonCompareMode.STRICT));
    }

    @Test
    void nutrientTotalsRoundedOnlyOnce() throws Exception {
        // Jede Zeile 0,04 g Eiweiß (gerundet 0,0); erst die Summe 0,12 ergibt gerundet 0,1.
        for (int i = 1; i <= 3; i++) {
            catalog.product("Gewürz " + i).sku("g" + i).packageGrams(100).nutrition("0.04", "1").insert();
        }

        summary(items("aldi:g1", "aldi:g2", "aldi:g3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].protein").value(0.0))
                .andExpect(jsonPath("$.totals.protein").value(0.1));
    }

    @Test
    void productWithoutCurrentPriceIsMissing() throws Exception {
        catalog.product("Roggenbrot").sku("1002").noPrice().insert();
        long superseded = catalog.product("Vollkornbrot").sku("1003").noPrice().insert();
        catalog.price(superseded, 189, "2026-11-01T10:00:00Z", false);

        summary(items("aldi:1002", "aldi:1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty())
                .andExpect(jsonPath("$.missingProductKeys[0]").value("aldi:1002"))
                .andExpect(jsonPath("$.missingProductKeys[1]").value("aldi:1003"));
    }

    @Test
    void malformedKeyIsMissing() throws Exception {
        summary(items("foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingProductKeys[0]").value("foo"));
    }

    @Test
    void goalsNullGivesNullProgress() throws Exception {
        catalog.product("Roggenbrot").sku("1002").insert();

        summary(items("aldi:1002"))
                .andExpect(status().isOk())
                // jsonPath(...).exists() gilt für einen null-Wert als nicht vorhanden; das Feld selbst prüft der String.
                .andExpect(content().string(containsString("\"goalProgress\":null")))
                .andExpect(jsonPath("$.goalProgress").value((Object) null));
    }

    @Test
    void partialGoals() throws Exception {
        catalog.product("Roggenbrot").sku("1002").price(200).insert();

        summary("""
                {"items":[{"productKey":"aldi:1002","quantity":1,"priceAtAddCents":200,"addedAt":"%s"}],
                 "goals":{"budgetCents":400}}""".formatted(ADDED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalProgress.budgetPct").value(50))
                .andExpect(jsonPath("$.goalProgress.proteinPct").value((Object) null));
    }

    @Test
    void emptyItems() throws Exception {
        summary("{\"items\":[]}")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"lines":[],
                         "totals":{"priceCents":0,"protein":0.0,"fat":0.0,"carbs":0.0,"kcal":0.0,
                                   "priceDifferenceCents":0},
                         "goalProgress":null,"missingProductKeys":[]}""", JsonCompareMode.STRICT));
    }

    @Test
    void exactly200Items() throws Exception {
        summary(items(IntStream.rangeClosed(1, 200).mapToObj(i -> "aldi:" + i).toArray(String[]::new)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingProductKeys.length()").value(200));
    }

    @Test
    void stateless() throws Exception {
        catalog.product("Roggenbrot").sku("1002").insert();
        String body = items("aldi:1002", "aldi:9999");

        String first = summary(body).andReturn().getResponse().getContentAsString();
        String second = summary(body).andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void missingItemsIs400() throws Exception {
        badRequest("{}", "items");
        badRequest("{\"items\":null}", "items");
    }

    @Test
    void moreThan200ItemsIs400() throws Exception {
        badRequest(items(IntStream.rangeClosed(1, 201).mapToObj(i -> "aldi:" + i).toArray(String[]::new)), "items");
    }

    @Test
    void nullItemIs400() throws Exception {
        badRequest("{\"items\":[null]}", "items");
    }

    @Test
    void duplicateKeyIs400() throws Exception {
        badRequest(items("aldi:1", "aldi:1"), "items");
    }

    @Test
    void quantityOutOfRangeIs400() throws Exception {
        badRequest(item("\"aldi:1\"", "0", "129", "\"" + ADDED + "\""), "quantity");
        badRequest(item("\"aldi:1\"", "1001", "129", "\"" + ADDED + "\""), "quantity");
    }

    @Test
    void priceAtAddOutOfRangeIs400() throws Exception {
        badRequest(item("\"aldi:1\"", "1", "0", "\"" + ADDED + "\""), "priceAtAddCents");
        badRequest(item("\"aldi:1\"", "1", "100001", "\"" + ADDED + "\""), "priceAtAddCents");
    }

    @Test
    void everyItemFieldIsRequired() throws Exception {
        badRequest(item("null", "1", "129", "\"" + ADDED + "\""), "productKey");
        badRequest(item("\"aldi:1\"", "null", "129", "\"" + ADDED + "\""), "quantity");
        badRequest(item("\"aldi:1\"", "1", "null", "\"" + ADDED + "\""), "priceAtAddCents");
        badRequest(item("\"aldi:1\"", "1", "129", "null"), "addedAt");
    }

    @Test
    void keyLongerThan64CharactersIs400() throws Exception {
        badRequest(items("aldi:" + "1".repeat(60)), "productKey");
    }

    @Test
    void goalOutOfRangeIs400() throws Exception {
        badRequest(withGoals("{\"budgetCents\":2147483648}"), "budgetCents");
        badRequest(withGoals("{\"protein\":0}"), "protein");
    }

    @Test
    void largestBudgetIsOk() throws Exception {
        summary(withGoals("{\"budgetCents\":2147483647}")).andExpect(status().isOk());
    }

    /**
     * Hält das heutige Verhalten fest: Jackson liest {@code 1.5} für eine ganze Zahl als {@code 1}
     * ({@code ACCEPT_FLOAT_AS_INT}), die Anfrage geht durch. Die Abhilfe ist globale Jackson-Konfiguration und gehört
     * zu #25; dann wird hieraus ein 400.
     */
    @Test
    void fractionalQuantityIsTruncatedUntil25() throws Exception {
        catalog.product("Roggenbrot").sku("1002").insert();

        summary(item("\"aldi:1002\"", "1.5", "129", "\"" + ADDED + "\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantity").value(1));
    }

    private ResultActions summary(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/shopping-list/summary")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void badRequest(String body, String field) throws Exception {
        summary(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).exists());
    }

    /** Zeilen mit Menge 1 und Preis 99 Cent beim Hinzufügen. */
    private static String items(String... keys) {
        return java.util.Arrays.stream(keys)
                .map(key -> "{\"productKey\":\"%s\",\"quantity\":1,\"priceAtAddCents\":99,\"addedAt\":\"%s\"}"
                        .formatted(key, ADDED))
                .collect(Collectors.joining(",", "{\"items\":[", "]}"));
    }

    /** Eine Zeile mit rohen JSON-Werten, etwa {@code null} oder {@code 1.5}. */
    private static String item(String productKey, String quantity, String priceAtAddCents, String addedAt) {
        return "{\"items\":[{\"productKey\":%s,\"quantity\":%s,\"priceAtAddCents\":%s,\"addedAt\":%s}]}"
                .formatted(productKey, quantity, priceAtAddCents, addedAt);
    }

    private static String withGoals(String goals) {
        return "{\"items\":[],\"goals\":%s}".formatted(goals);
    }
}
