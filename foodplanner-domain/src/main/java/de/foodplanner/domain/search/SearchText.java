package de.foodplanner.domain.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Suchtext der lexikalischen Produktsuche (§5.5): der Rohtext und seine Tokens.
 *
 * <p>Die Tokens entstehen aus dem gefalteten Text ({@link #fold(String)}) durch Trennen an allem, was weder Buchstabe
 * noch Ziffer ist. Leere Teile und Duplikate entfallen, die Reihenfolge bleibt. Sonderzeichen gelangen so nie in ein
 * Token. Bleibt kein Token übrig, gilt die Anfrage als leer.
 *
 * @param raw    der Text, wie ihn der Aufrufer geschickt hat
 * @param tokens gefaltete, eindeutige Tokens in Eingabereihenfolge, unveränderlich
 */
public record SearchText(String raw, List<String> tokens) {

    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    /**
     * Zeichen, die sich nicht per NFD in Grundbuchstabe und Akzent zerlegen lassen. Sie müssen genauso falten wie
     * PostgreSQL {@code unaccent}, damit Java und SQL dieselben Tokens sehen. Die Tabelle wächst, bis der Abgleich
     * gegen {@code unaccent} stimmt.
     */
    private static final Map<Character, String> EXCEPTIONS = Map.of('ß', "ss");

    public SearchText {
        tokens = List.copyOf(tokens);
    }

    /**
     * Faltet den Text und zerlegt ihn in Tokens.
     */
    public static SearchText parse(String raw) {
        var distinct = new LinkedHashSet<String>();
        for (String part : SEPARATORS.split(fold(raw))) {
            if (!part.isEmpty()) {
                distinct.add(part);
            }
        }
        return new SearchText(raw, new ArrayList<>(distinct));
    }

    /**
     * Genau dann {@code true}, wenn es keine Tokens gibt.
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    /**
     * Faltet in der Reihenfolge des SQL-Ausdrucks {@code immutable_unaccent(lower(…))}: zuerst die Ausnahmetabelle,
     * dann Kleinschreibung ({@link Locale#ROOT}), NFD und Entfernen der Akzentzeichen.
     */
    public static String fold(String s) {
        var mapped = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String replacement = EXCEPTIONS.get(c);
            if (replacement == null) {
                mapped.append(c);
            } else {
                mapped.append(replacement);
            }
        }
        String decomposed = Normalizer.normalize(mapped.toString().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("");
    }
}
