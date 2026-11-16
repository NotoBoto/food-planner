package de.foodplanner.application.products.search;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.port.PageRequest;
import de.foodplanner.application.port.SearchFilter;
import de.foodplanner.application.port.SearchSort;

import static org.assertj.core.api.Assertions.assertThat;

class SearchProductsHandlerTest {

    private final FakeProductSearch search = new FakeProductSearch();
    private final SearchProductsHandler handler = new SearchProductsHandler(search);

    private static SearchProductsQuery query(String q) {
        return new SearchProductsQuery(q, null, null, null, null, SearchSort.RELEVANCE, 1, 20);
    }

    @Test
    void eightDigitsIsEanSearch() {
        handler.handle(query("40084004"));

        assertThat(search.method).isEqualTo("byOffCode");
        assertThat(search.offCode).isEqualTo("40084004");
        assertThat(search.page).isEqualTo(new PageRequest(1, 20));
    }

    @Test
    void thirteenDigitsIsEanSearch() {
        handler.handle(query(" 4008400401211 "));

        assertThat(search.method).isEqualTo("byOffCode");
        assertThat(search.offCode).isEqualTo("4008400401211");
        assertThat(search.page).isEqualTo(new PageRequest(1, 20));
    }

    @Test
    void eanIgnoresFilters() {
        handler.handle(new SearchProductsQuery("4008400401211", "Obst", 100L, null, null, SearchSort.PRICE_ASC, 1, 20));

        assertThat(search.method).isEqualTo("byOffCode");
        assertThat(search.calls).isEqualTo(1);
    }

    @Test
    void twelveDigitsIsLexical() {
        handler.handle(query("400840040121"));

        assertThat(search.method).isEqualTo("text");
        assertThat(search.text.tokens()).containsExactly("400840040121");
    }

    @Test
    void blankQueryBrowses() {
        SearchFilter filter = new SearchFilter("Obst", 100L, 500L, new BigDecimal("5"));
        for (String q : new String[] {null, "  ", "%,"}) {
            handler.handle(new SearchProductsQuery(q, "Obst", 100L, 500L, new BigDecimal("5"),
                    SearchSort.PRICE_DESC, 2, 10));

            assertThat(search.method).isEqualTo("browse");
            assertThat(search.filter).isEqualTo(filter);
            assertThat(search.sort).isEqualTo(SearchSort.PRICE_DESC);
            assertThat(search.page).isEqualTo(new PageRequest(2, 10));
        }
        assertThat(search.calls).isEqualTo(3);
    }

    @Test
    void textQueryIsLexicalWithFoldedTokens() {
        handler.handle(new SearchProductsQuery("Milch Fettarm", "Milch", null, 300L, null,
                SearchSort.KCAL_ASC, 1, 20));

        assertThat(search.method).isEqualTo("text");
        assertThat(search.text.tokens()).isEqualTo(List.of("milch", "fettarm"));
        assertThat(search.text.raw()).isEqualTo("Milch Fettarm");
        assertThat(search.filter).isEqualTo(new SearchFilter("Milch", null, 300L, null));
        assertThat(search.sort).isEqualTo(SearchSort.KCAL_ASC);
    }

    @Test
    void blankCategoryMeansNoFilter() {
        for (String category : new String[] {"", "  "}) {
            handler.handle(new SearchProductsQuery("Milch", category, null, null, null, SearchSort.RELEVANCE, 1, 20));
            assertThat(search.filter.category()).isNull();

            handler.handle(new SearchProductsQuery(null, category, null, null, null, SearchSort.RELEVANCE, 1, 20));
            assertThat(search.filter.category()).isNull();
        }
    }
}
