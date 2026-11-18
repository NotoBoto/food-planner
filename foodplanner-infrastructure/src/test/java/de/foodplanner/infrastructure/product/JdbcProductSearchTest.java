package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.application.port.PageRequest;
import de.foodplanner.application.port.PagedResult;
import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.application.port.SearchFilter;
import de.foodplanner.application.port.SearchSort;
import de.foodplanner.domain.search.SearchText;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * {@link JdbcProductSearch} gegen PostgreSQL mit pg_trgm: lexikalische Suche mit Score (§5.5), EAN-Suche, Filter,
 * Sortierungen, Deckel von 200 Treffern und Seiten (§5.1). Sichtbar ist nur, was {@link VisibleProducts} zulässt
 * (§4.3).
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import({PostgresTestConfiguration.class, JdbcProductSearch.class})
@Transactional
class JdbcProductSearchTest {

    private static final PageRequest PAGE1 = new PageRequest(1, 20);
    private static final SearchFilter NONE = SearchFilter.none();

    @Autowired
    JdbcProductSearch search;

    @Autowired
    JdbcClient jdbc;

    private int skuCounter;

    // ---------------------------------------------------------------- Abgleich

    @Test
    void tokensMatchInAnyOrder() {
        long id = product("Fettarme Milch 1,5 %").insert();

        assertThat(ids(text("milch fettarm"))).containsExactly(id);
    }

    @Test
    void everyTokenMustMatch() {
        product("Fettarme Milch 1,5 %").insert();

        assertThat(text("milch schokolade").items()).isEmpty();
    }

    @Test
    void tokenMatchesInsideCompound() {
        long id = product("Vollmilchschokolade").insert();

        assertThat(ids(text("schokolade"))).containsExactly(id);
    }

    @Test
    void accentInsensitive() {
        long id = product("Caffè Crema").insert();

        assertThat(ids(text("caffe"))).containsExactly(id);
        assertThat(ids(text("CAFFÈ"))).containsExactly(id);
    }

    @Test
    void matchesBrandAndCategory() {
        long byBrand = product("Joghurt Natur").brand("Milbona").insert();
        long byCategory = product("Gouda jung").category("Molkereiprodukte").insert();

        assertThat(ids(text("milbona"))).containsExactly(byBrand);
        assertThat(ids(text("molkerei"))).containsExactly(byCategory);
    }

    @Test
    void nameOutranksBrandOutranksCategory() {
        long c = product("Apfelsaft").category("Bio").insert();
        long b = product("Haferflocken").brand("Bio").insert();
        long a = product("Bio Hafer").insert();

        assertThat(ids(text("bio"))).containsExactly(a, b, c);
    }

    @Test
    void exactWordOutranksPrefixOutranksInfix() {
        long infix = product("Vollmilch").insert();
        long prefix = product("Milchreis").insert();
        long exact = product("Milch frisch").insert();

        assertThat(ids(text("milch"))).containsExactly(exact, prefix, infix);
    }

    /**
     * Nur der Bonus für ein exaktes Wort (+0,5) erklärt die Reihenfolge: Ohne Bonus läge B bei 0,6 × 1,0 = 0,6 und
     * mit nur dem Wortanfangsbonus bei 0,6 × 1,25 = 0,75, beides unter A (Infix, ≈ 0,818). Alphabetisch käme A
     * ebenfalls zuerst. Mit dem Bonus liegt B bei 0,6 × 1,5 = 0,9.
     */
    @Test
    void exactWordBonusLiftsBrandAboveBetterRawName() {
        double nameA = wordSimilarity("schokolade", "vollmilchschokolade");
        double brandB = wordSimilarity("schokolade", "schokolade");
        assertThat(0.6 * (brandB + 0.25)).isLessThan(nameA);
        assertThat(0.6 * (brandB + 0.5)).isGreaterThan(nameA);
        assertThat(wordSimilarity("schokolade", "zwieback")).isZero();
        long a = product("Vollmilchschokolade").insert();
        long b = product("Zwieback").brand("Schokolade").insert();

        assertThat(ids(text("schokolade"))).containsExactly(b, a);
    }

    /**
     * Nur der Wortanfangsbonus (+0,25) erklärt die Reihenfolge: Ohne Bonus läge B bei 0,6 × 0,857 ≈ 0,514, unter A
     * (Infix, ≈ 0,571), und alphabetisch käme A ebenfalls zuerst. Mit dem Bonus liegt B bei 0,6 × 1,107 ≈ 0,664.
     */
    @Test
    void wordStartBonusLiftsBrandAboveBetterRawName() {
        double nameA = wordSimilarity("butter", "erdnussbutterkekse");
        double brandB = wordSimilarity("butter", "butterland");
        assertThat(nameA).isGreaterThanOrEqualTo(0.4);
        assertThat(0.6 * brandB).isLessThan(nameA);
        assertThat(0.6 * (brandB + 0.25)).isGreaterThan(nameA);
        assertThat(wordSimilarity("butter", "zwieback")).isZero();
        long a = product("Erdnussbutterkekse").insert();
        long b = product("Zwieback").brand("Butterland").insert();

        assertThat(ids(text("butter"))).containsExactly(b, a);
    }

    @Test
    void productWithoutBrandOrCategoryIsFoundByName() {
        long id = product("Haferflocken zart").insert();

        assertThat(ids(text("haferflocken"))).containsExactly(id);
        assertThat(text("haferflocken").items().getFirst().brand()).isNull();
        assertThat(text("haferflocken").items().getFirst().category()).isNull();
    }

    @Test
    void specialCharactersInQueryAreHarmless() {
        product("O'Neil Kekse").insert();

        assertThatCode(() -> text("o'neil c++ 50% \\ (")).doesNotThrowAnyException();
        assertThat(text("o'neil c++ 50% \\ (").totalCount()).isZero();
    }

    @Test
    void emptyTextIsRejectedBecauseRoutingBelongsToTheUseCase() {
        assertThatThrownBy(() -> search.text(SearchText.parse(" %! "), NONE, SearchSort.RELEVANCE, PAGE1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invisibleProductsNeverMatch() {
        long superseded = product("Milch alt").offCode("4000000000001").noPrice().insert();
        insertPrice(superseded, 99, false);
        product("Milch ohne Preis").offCode("4000000000002").noPrice().insert();

        assertThat(text("milch").totalCount()).isZero();
        assertThat(search.browse(NONE, SearchSort.RELEVANCE, PAGE1).totalCount()).isZero();
        assertThat(search.byOffCode("4000000000001", PAGE1).totalCount()).isZero();
        assertThat(search.byOffCode("4000000000002", PAGE1).totalCount()).isZero();
    }

    @Test
    void thresholdAppliesPerToken() {
        assertThat(wordSimilarity("jogurt", "joghurt natur")).isGreaterThanOrEqualTo(0.4).isLessThan(0.6);
        assertThat(wordSimilarity("yogurt", "joghurt natur")).isLessThan(0.4);
        long id = product("Joghurt Natur").insert();

        assertThat(ids(text("jogurt"))).containsExactly(id);
        assertThat(text("yogurt").items()).isEmpty();
        assertThat(text("jogurt milch").items()).isEmpty();
    }

    // ------------------------------------------------- EAN, Filter, Sortierung, Seiten

    @Test
    void eanMatchesExactlyOffCode() {
        long id = product("Haferdrink").offCode("4008400401211").insert();
        product("Haferdrink Barista").offCode("4008400401212").insert();

        assertThat(ids(search.byOffCode("4008400401211", PAGE1))).containsExactly(id);
        PagedResult<ProductSummary> unknown = search.byOffCode("0000000000000", PAGE1);
        assertThat(unknown.items()).isEmpty();
        assertThat(unknown.totalCount()).isZero();
        assertThat(unknown.totalPages()).isZero();
    }

    @Test
    void eanPagingAppliesToo() {
        product("Haferdrink").offCode("4008400401211").insert();

        PagedResult<ProductSummary> page2 = search.byOffCode("4008400401211", new PageRequest(2, 20));
        assertThat(page2.items()).isEmpty();
        assertThat(page2.totalCount()).isEqualTo(1);
    }

    @Test
    void browseSortsByName() {
        long c = product("Zwieback").insert();
        long a = product("Apfel").insert();
        long b = product("Äpfel getrocknet").insert(); // gefaltet „apfel getrocknet“

        assertThat(ids(search.browse(NONE, SearchSort.RELEVANCE, PAGE1))).containsExactly(a, b, c);
    }

    @Test
    void filtersApplyInQuery() {
        long cheapDairy = product("Milch fettarm").category("Molkerei").price(99).nutrition("3.4", "47").insert();
        long dearDairy = product("Milch Bio").category("Molkerei").price(189).nutrition("3.3", "64").insert();
        long cheapOther = product("Milchbrötchen").category("Backwaren").price(129).nutrition("8.0", "330").insert();
        long noNutrition = product("Milchreis").category("Molkerei").price(149).insert();

        // einzeln
        assertFilter(new SearchFilter("Molkerei", null, null, null), cheapDairy, dearDairy, noNutrition);
        assertFilter(new SearchFilter(null, 129L, null, null), dearDairy, cheapOther, noNutrition);
        assertFilter(new SearchFilter(null, null, 129L, null), cheapDairy, cheapOther);
        assertFilter(new SearchFilter(null, null, null, new BigDecimal("3.4")), cheapDairy, cheapOther);
        // kombiniert
        assertFilter(new SearchFilter("Molkerei", 100L, 189L, null), dearDairy, noNutrition);
        assertFilter(new SearchFilter("Molkerei", null, 150L, new BigDecimal("3.0")), cheapDairy);
        assertFilter(new SearchFilter("Gibt es nicht", null, null, null));
    }

    @Test
    void productsWithoutNutritionSortLastAndFailProteinFilter() {
        long without = product("Milch A").insert();
        long low = product("Milch B").nutrition("3.0", "64").insert();
        long high = product("Milch C").nutrition("8.0", "40").insert();

        assertThat(ids(text("milch", SearchSort.PROTEIN_DESC))).containsExactly(high, low, without);
        assertThat(ids(text("milch", SearchSort.KCAL_ASC))).containsExactly(high, low, without);
        assertThat(ids(search.browse(NONE, SearchSort.PROTEIN_DESC, PAGE1))).containsExactly(high, low, without);
        assertThat(ids(search.browse(NONE, SearchSort.KCAL_ASC, PAGE1))).containsExactly(high, low, without);
        assertThat(ids(search.browse(new SearchFilter(null, null, null, BigDecimal.ZERO), SearchSort.RELEVANCE,
                PAGE1))).containsExactly(low, high);
    }

    @Test
    void sortsReorderTheSameHits() {
        long a = product("Milch frisch").price(149).nutrition("3.4", "64").insert();
        long b = product("Milchreis").price(99).nutrition("3.0", "110").insert();
        long c = product("Vollmilch").price(119).nutrition("3.5", "47").insert();
        product("Haferdrink").price(50).nutrition("1.0", "40").insert();

        assertThat(ids(text("milch", SearchSort.RELEVANCE))).containsExactly(a, b, c);
        assertThat(ids(text("milch", SearchSort.PRICE_ASC))).containsExactly(b, c, a);
        assertThat(ids(text("milch", SearchSort.PRICE_DESC))).containsExactly(a, c, b);
        assertThat(ids(text("milch", SearchSort.PROTEIN_DESC))).containsExactly(c, a, b);
        assertThat(ids(text("milch", SearchSort.KCAL_ASC))).containsExactly(c, a, b);
    }

    @Test
    void relevanceIsCappedAt200() {
        List<Long> skus = new ArrayList<>();
        for (int i = 1; i <= 205; i++) {
            skus.add((long) i);
        }
        jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        SELECT 'cap-' || n, 'Milch ' || n, 1000, 'G', 'ALDI' FROM unnest(CAST(:n AS bigint[])) n""")
                .param("n", skus.toArray(Long[]::new))
                .update();
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        SELECT id, 100 + id % 50, 'aldi-sued.de', now(), true FROM product WHERE aldi_sku LIKE 'cap-%'""")
                .update();

        PagedResult<ProductSummary> relevance =
                search.text(SearchText.parse("milch"), NONE, SearchSort.RELEVANCE, new PageRequest(1, 100));
        assertThat(relevance.totalCount()).isEqualTo(200);
        assertThat(relevance.totalPages()).isEqualTo(2);
        assertThat(relevance.items()).hasSize(100);

        PagedResult<ProductSummary> byPrice =
                search.text(SearchText.parse("milch"), NONE, SearchSort.PRICE_ASC, new PageRequest(1, 100));
        assertThat(byPrice.totalCount()).isEqualTo(200);
        assertThat(byPrice.totalPages()).isEqualTo(2);

        PagedResult<ProductSummary> byPricePage3 =
                search.text(SearchText.parse("milch"), NONE, SearchSort.PRICE_ASC, new PageRequest(3, 100));
        assertThat(byPricePage3.items()).isEmpty();
        assertThat(byPricePage3.totalCount()).isEqualTo(200);
    }

    /**
     * Erst der Deckel nach Score, dann die Umsortierung (Entscheidung 7): Fünf billige Treffer mit niedrigem Score
     * („Vollmilch n“, nur Infix) fallen aus den 200 besten und dürfen auch bei {@code PRICE_ASC} nicht auftauchen,
     * obwohl sie die billigsten sind. Würde zuerst nach Preis sortiert und dann gedeckelt, stünden sie vorne.
     */
    @Test
    void capSelectsByScoreBeforeOtherSortsReorder() {
        List<Long> skus = new ArrayList<>();
        for (int i = 1; i <= 205; i++) {
            skus.add((long) i);
        }
        jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, package_grams, unit, source)
                        SELECT 'cap-' || n, 'Milch ' || n, 1000, 'G', 'ALDI' FROM unnest(CAST(:n AS bigint[])) n""")
                .param("n", skus.toArray(Long[]::new))
                .update();
        List<Long> cheapLowScore = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            cheapLowScore.add(product("Vollmilch " + i).price(10).insert());
        }
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        SELECT id, 100 + id % 50, 'aldi-sued.de', now(), true FROM product WHERE aldi_sku LIKE 'cap-%'""")
                .update();
        // Vorbedingung: „milch“ ist in „Milch n“ ein exaktes Wort, in „Vollmilch n“ nur Infix.
        assertThat(wordSimilarity("milch", "milch 1") + 0.5).isGreaterThan(wordSimilarity("milch", "vollmilch 1"));

        List<Long> relevanceIds = allIds(SearchSort.RELEVANCE);
        List<Long> priceIds = allIds(SearchSort.PRICE_ASC);

        assertThat(relevanceIds).hasSize(200).doesNotContainAnyElementsOf(cheapLowScore);
        assertThat(priceIds).hasSize(200).doesNotContainAnyElementsOf(cheapLowScore)
                .containsExactlyInAnyOrderElementsOf(relevanceIds);
        assertThat(search.text(SearchText.parse("milch"), NONE, SearchSort.PRICE_ASC, new PageRequest(1, 100))
                .items().getFirst().priceCents()).isGreaterThanOrEqualTo(100);
    }

    private List<Long> allIds(SearchSort sort) {
        List<Long> all = new ArrayList<>();
        for (int page = 1; page <= 2; page++) {
            all.addAll(ids(search.text(SearchText.parse("milch"), NONE, sort, new PageRequest(page, 100))));
        }
        return all;
    }

    @Test
    void pageBeyondEndIsEmptyButKeepsTotal() {
        product("Milch 1").insert();
        product("Milch 2").insert();
        product("Milch 3").insert();

        PagedResult<ProductSummary> textPage = search.text(SearchText.parse("milch"), NONE, SearchSort.RELEVANCE,
                new PageRequest(3, 2));
        assertThat(textPage.items()).isEmpty();
        assertThat(textPage.totalCount()).isEqualTo(3);
        assertThat(textPage.totalPages()).isEqualTo(2);

        PagedResult<ProductSummary> browsePage = search.browse(NONE, SearchSort.RELEVANCE, new PageRequest(2, 2));
        assertThat(browsePage.items()).hasSize(1);
        assertThat(browsePage.totalCount()).isEqualTo(3);

        PagedResult<ProductSummary> nothing = search.text(SearchText.parse("zwieback"), NONE, SearchSort.RELEVANCE,
                PAGE1);
        assertThat(nothing.totalCount()).isZero();
        assertThat(nothing.totalPages()).isZero();
    }

    @Test
    void summaryIsMapped() {
        long id = product("Orangensaft").sku("4711").offCode("4061458000001").brand("Rio d'Oro")
                .category("Säfte").unit("ML").packageGrams(750).price(129)
                .nutrition("1.55", "43", "0.14", "9.06").insert();

        assertThat(text("orangensaft").items()).containsExactly(new ProductSummary(id, "aldi:4711", "Orangensaft",
                "Rio d'Oro", "Säfte", 750, "ML", 129, 172, new BigDecimal("1.6"), new BigDecimal("0.1"),
                new BigDecimal("9.1"), new BigDecimal("43.0")));
    }

    @Test
    void summaryKeyFallsBackToOffCode() {
        long id = product("Haferdrink").noSku().offCode("4008400401211").insert();

        assertThat(search.byOffCode("4008400401211", PAGE1).items().getFirst().productKey())
                .isEqualTo("off:4008400401211");
        assertThat(search.byOffCode("4008400401211", PAGE1).items().getFirst().id()).isEqualTo(id);
    }

    // ---------------------------------------------------------------- Helfer

    private PagedResult<ProductSummary> text(String query) {
        return text(query, SearchSort.RELEVANCE);
    }

    private PagedResult<ProductSummary> text(String query, SearchSort sort) {
        return search.text(SearchText.parse(query), NONE, sort, PAGE1);
    }

    private void assertFilter(SearchFilter filter, Long... expected) {
        assertThat(ids(search.browse(filter, SearchSort.RELEVANCE, PAGE1)))
                .as("browse %s", filter).containsExactlyInAnyOrder(expected);
        assertThat(ids(search.text(SearchText.parse("milch"), filter, SearchSort.RELEVANCE, PAGE1)))
                .as("text %s", filter).containsExactlyInAnyOrder(expected);
    }

    private static List<Long> ids(PagedResult<ProductSummary> result) {
        return result.items().stream().map(ProductSummary::id).toList();
    }

    private double wordSimilarity(String token, String name) {
        return jdbc.sql("SELECT word_similarity(:t, :name)")
                .param("t", token)
                .param("name", name)
                .query(Double.class)
                .single();
    }

    private void insertPrice(long productId, long cents, boolean current) {
        jdbc.sql("""
                        INSERT INTO price (product_id, price_cents, source_name, scraped_at, is_current)
                        VALUES (:productId, :cents, 'aldi-sued.de', now(), :current)""")
                .param("productId", productId)
                .param("cents", cents)
                .param("current", current)
                .update();
    }

    private ProductBuilder product(String name) {
        return new ProductBuilder(name, "sku-" + ++skuCounter);
    }

    /** Legt ein Produkt an, standardmäßig mit aktuellem Preis (sichtbar), 500 g, ohne Nährwerte. */
    private final class ProductBuilder {

        private final String name;
        private @Nullable String sku;
        private @Nullable String offCode;
        private @Nullable String brand;
        private @Nullable String category;
        private int packageGrams = 500;
        private String unit = "G";
        private @Nullable Long priceCents = 199L;
        private @Nullable BigDecimal protein;
        private @Nullable BigDecimal kcal;
        private @Nullable BigDecimal fat;
        private @Nullable BigDecimal carbs;

        ProductBuilder(String name, String sku) {
            this.name = name;
            this.sku = sku;
        }

        ProductBuilder sku(String value) {
            sku = value;
            return this;
        }

        ProductBuilder noSku() {
            sku = null;
            return this;
        }

        ProductBuilder offCode(String value) {
            offCode = value;
            return this;
        }

        ProductBuilder brand(String value) {
            brand = value;
            return this;
        }

        ProductBuilder category(String value) {
            category = value;
            return this;
        }

        ProductBuilder packageGrams(int value) {
            packageGrams = value;
            return this;
        }

        ProductBuilder unit(String value) {
            unit = value;
            return this;
        }

        ProductBuilder price(long cents) {
            priceCents = cents;
            return this;
        }

        ProductBuilder noPrice() {
            priceCents = null;
            return this;
        }

        /** Protein und kcal; Fett und Kohlenhydrate erhalten Platzhalter, denn es gilt „alle vier oder keiner“. */
        ProductBuilder nutrition(String protein100, String kcal100) {
            return nutrition(protein100, kcal100, "1.0", "1.0");
        }

        ProductBuilder nutrition(String protein100, String kcal100, String fat100, String carbs100) {
            protein = new BigDecimal(protein100);
            kcal = new BigDecimal(kcal100);
            fat = new BigDecimal(fat100);
            carbs = new BigDecimal(carbs100);
            return this;
        }

        long insert() {
            long id = jdbc.sql("""
                            INSERT INTO product (aldi_sku, off_code, name, brand, category, package_grams, unit,
                                                 kcal100, protein100, fat100, carbs100, source)
                            VALUES (:sku, :offCode, :name, :brand, :category, :packageGrams, :unit,
                                    :kcal, :protein, :fat, :carbs, 'ALDI')
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
                    .query(Long.class)
                    .single();
            if (priceCents != null) {
                insertPrice(id, priceCents, true);
            }
            return id;
        }
    }
}
