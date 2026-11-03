package de.foodplanner.infrastructure.product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import de.foodplanner.application.port.PageRequest;
import de.foodplanner.application.port.PagedResult;
import de.foodplanner.application.port.ProductSearch;
import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.application.port.SearchFilter;
import de.foodplanner.application.port.SearchSort;
import de.foodplanner.domain.search.SearchText;

/**
 * {@link ProductSearch} über PostgreSQL mit pg_trgm (§5.5). Sichtbar ist nur, was {@link VisibleProducts#FROM}
 * zulässt (§4.3).
 *
 * <p>Jeder Weg baut eine CTE {@code hits} und stellt darauf zwei Abfragen: die Zählung und die Seite. Beide teilen
 * damit dasselbe {@code WHERE}. Werte gehen nur als benannte Parameter hinein. Im SQL-Text entstehen in Java nur die
 * {@code ORDER BY}-Klausel (aus {@link SearchSort}) und die Zahl der Token-Platzhalter {@code :t0}, {@code :t1}, ….
 *
 * <p>Lexikalisch trifft ein Produkt, wenn jedes Token per {@code <%} (Schwelle
 * {@code pg_trgm.word_similarity_threshold} = 0,4, gesetzt je Pool-Verbindung) Name, Marke oder Kategorie trifft. Der
 * Score ist die Summe der Tokenscores; ein Tokenscore ist das beste Feld nach
 * {@code Gewicht × (word_similarity + Bonus)} mit den Gewichten 1,0 (Name), 0,6 (Marke), 0,4 (Kategorie) und dem
 * Bonus +0,5 für ein exaktes Wort, sonst +0,25 für einen Wortanfang. Die Treffermenge sind die 200 besten nach Score;
 * die übrigen Sortierungen ordnen genau diese Menge um.
 */
@Repository
class JdbcProductSearch implements ProductSearch {

    /** Höchstzahl lexikalischer Treffer (§5.5). */
    static final int TEXT_HIT_LIMIT = 200;

    private static final String COLUMNS = """
            p.id, p.aldi_sku, p.off_code, p.name, p.brand, p.category, p.package_grams, p.unit, cp.price_cents, \
            p.protein100, p.fat100, p.carbs100, p.kcal100, p.search_name""";

    private final JdbcClient jdbc;

    JdbcProductSearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PagedResult<ProductSummary> byOffCode(String offCode, PageRequest page) {
        // Nur die Sichtbarkeitsregel, keine Filter (§5.4).
        Map<String, Object> params = new HashMap<>();
        params.put("ean", offCode);
        String hits = "SELECT " + COLUMNS + " FROM " + VisibleProducts.FROM + " WHERE p.off_code = :ean";
        return run(hits, params, orderBy(SearchSort.RELEVANCE, false), page);
    }

    @Override
    public PagedResult<ProductSummary> browse(SearchFilter filter, SearchSort sort, PageRequest page) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        addFilter(filter, conditions, params);
        String hits = "SELECT " + COLUMNS + " FROM " + VisibleProducts.FROM + where(conditions);
        return run(hits, params, orderBy(sort, false), page);
    }

    @Override
    public PagedResult<ProductSummary> text(SearchText text, SearchFilter filter, SearchSort sort, PageRequest page) {
        if (text.isEmpty()) {
            // Leere Anfrage: kein Limit, RELEVANCE heißt Sortierung nach Name (§5.5).
            return browse(filter, sort, page);
        }
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        List<String> tokenScores = new ArrayList<>();
        List<String> tokens = text.tokens();
        for (int i = 0; i < tokens.size(); i++) {
            String t = ":t" + i;
            params.put("t" + i, tokens.get(i));
            conditions.add("(" + t + " <% p.search_name OR " + t + " <% p.search_brand OR "
                    + t + " <% p.search_category)");
            // GREATEST ignoriert NULL: fehlen Marke oder Kategorie, zählt das beste vorhandene Feld.
            tokenScores.add("GREATEST(" + fieldScore("p.search_name", "1.0", t) + ", "
                    + fieldScore("p.search_brand", "0.6", t) + ", "
                    + fieldScore("p.search_category", "0.4", t) + ")");
        }
        addFilter(filter, conditions, params);
        String hits = "SELECT " + COLUMNS + ", " + String.join(" + ", tokenScores) + " AS score"
                + " FROM " + VisibleProducts.FROM + where(conditions)
                + " ORDER BY score DESC, p.search_name, p.id LIMIT " + TEXT_HIT_LIMIT;
        return run(hits, params, orderBy(sort, true), page);
    }

    /**
     * {@code w × (word_similarity + Bonus)}. Die Tokens bestehen nur aus Buchstaben und Ziffern
     * ({@link SearchText}), deshalb ist {@code '\m' || :t || '\M'} ein gültiges Wortmuster ohne Sonderzeichen.
     */
    private static String fieldScore(String column, String weight, String token) {
        return weight + " * (word_similarity(" + token + ", " + column + ") + CASE"
                + " WHEN " + column + " ~ ('\\m' || " + token + " || '\\M') THEN 0.5"
                + " WHEN " + column + " ~ ('\\m' || " + token + ") THEN 0.25"
                + " ELSE 0 END)";
    }

    /** Jede Bedingung nur, wenn der Wert gesetzt ist; ein gesetztes Mindestprotein schließt {@code NULL} aus. */
    private static void addFilter(SearchFilter filter, List<String> conditions, Map<String, Object> params) {
        if (filter.category() != null) {
            conditions.add("p.category = :category");
            params.put("category", filter.category());
        }
        if (filter.minPriceCents() != null) {
            conditions.add("cp.price_cents >= :minPrice");
            params.put("minPrice", filter.minPriceCents());
        }
        if (filter.maxPriceCents() != null) {
            conditions.add("cp.price_cents <= :maxPrice");
            params.put("maxPrice", filter.maxPriceCents());
        }
        if (filter.minProtein100g() != null) {
            conditions.add("p.protein100 >= :minProtein");
            params.put("minProtein", filter.minProtein100g());
        }
    }

    private static String where(List<String> conditions) {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    /**
     * Die Sortierung über den Spalten von {@code hits}; bei Gleichstand immer {@code search_name}, dann {@code id}.
     * Ohne Text bedeutet {@code RELEVANCE} Sortierung nach Name.
     */
    private static String orderBy(SearchSort sort, boolean lexical) {
        String primary = switch (sort) {
            case RELEVANCE -> lexical ? "score DESC, " : "";
            case PRICE_ASC -> "price_cents ASC, ";
            case PRICE_DESC -> "price_cents DESC, ";
            case PROTEIN_DESC -> "protein100 DESC NULLS LAST, ";
            case KCAL_ASC -> "kcal100 ASC NULLS LAST, ";
        };
        return primary + "search_name, id";
    }

    private PagedResult<ProductSummary> run(String hits, Map<String, Object> params, String orderBy,
            PageRequest page) {
        String with = "WITH hits AS (" + hits + ") ";
        long total = jdbc.sql(with + "SELECT count(*) FROM hits")
                .params(params)
                .query(Long.class)
                .single();
        if (page.offset() >= total) {
            return PagedResult.of(List.of(), page, total);
        }
        List<ProductSummary> items = jdbc.sql(with + "SELECT * FROM hits ORDER BY " + orderBy
                        + " LIMIT :pageSize OFFSET :offset")
                .params(params)
                .param("pageSize", page.pageSize())
                .param("offset", page.offset())
                .query(ProductSummaryRowMapper.INSTANCE)
                .list();
        return PagedResult.of(items, page, total);
    }
}
