package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import de.foodplanner.domain.search.SearchText;
import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * Die Java-Faltung {@link SearchText#fold(String)} muss wie der SQL-Ausdruck der generierten Suchspalten falten (§5.5),
 * sonst finden Tokens aus Java die Spaltenwerte nicht.
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import(PostgresTestConfiguration.class)
@Transactional
class SearchFoldingTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void javaFoldingMatchesUnaccent() {
        var mismatches = new ArrayList<String>();
        var letters = new ArrayList<Character>();
        for (char c = 'À'; c <= 'ɏ'; c++) {
            // Ausnahme Ʀ (U+01A6): unaccent.rules bildet ʀ, den Kleinbuchstaben von Ʀ, auf großes R ab. Der Buchstabe
            // kommt in einem deutschen Lebensmittelkatalog nicht vor, und pg_trgm vergleicht ohnehin ohne Groß-/Kleinschreibung.
            if (Character.isLetter(c) && c != 'Ʀ') {
                letters.add(c);
            }
        }
        letters.add('ẞ'); // ẞ, großes Eszett
        for (char c : letters) {
            String input = String.valueOf(c);
            String java = SearchText.fold(input);
            String sql = jdbc.sql("SELECT immutable_unaccent(lower(CAST(:s AS text) COLLATE pg_c_utf8))")
                    .param("s", input)
                    .query(String.class)
                    .single();
            if (!java.equals(sql)) {
                mismatches.add("U+%04X -> (java=%s, sql=%s)".formatted((int) c, codepoints(java), codepoints(sql)));
            }
        }
        assertThat(mismatches).as("Abweichungen Java gegen unaccent").isEmpty();
    }

    private static String codepoints(String s) {
        return s.codePoints().mapToObj("U+%04X"::formatted).collect(java.util.stream.Collectors.joining(" "));
    }

    @Test
    void generatedColumnsAreFolded() {
        var row = jdbc.sql("""
                        INSERT INTO product (aldi_sku, name, brand, category, package_grams, unit, source)
                        VALUES ('2001', 'Caffè Crème', 'Müller', 'Getränke', 500, 'G', 'ALDI')
                        RETURNING search_name, search_brand, search_category""")
                .query((rs, n) -> new String[] {
                        rs.getString("search_name"), rs.getString("search_brand"), rs.getString("search_category")})
                .single();

        assertThat(row).containsExactly("caffe creme", "muller", "getranke");
    }
}
