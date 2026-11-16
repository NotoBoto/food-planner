package de.foodplanner.domain.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    /**
     * @throws IllegalArgumentException wenn ein Token nicht nur aus Buchstaben und Ziffern besteht; die Regex-Muster
     *                                  der Suche verlassen sich darauf
     */
    public SearchText {
        tokens = List.copyOf(tokens);
        for (String token : tokens) {
            if (!TOKEN.matcher(token).matches()) {
                throw new IllegalArgumentException("Token darf nur Buchstaben und Ziffern enthalten: " + token);
            }
        }
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
     * Faltet wie der SQL-Ausdruck {@code immutable_unaccent(lower(…))}: zuerst Kleinschreibung ({@link Locale#ROOT}),
     * dann je Zeichen die Ausnahmetabelle {@link UnaccentExceptions}, sonst NFD, am Ende Entfernen der Akzentzeichen.
     * Ein Tabellenwert bleibt unzerlegt, denn {@code unaccent} lässt manche Zeichen unverändert, die NFD zerlegen
     * würde (etwa ǣ). Die Tabelle braucht deshalb nur Kleinbuchstaben als Schlüssel.
     */
    public static String fold(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        var folded = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            String replacement = UnaccentExceptions.MAP.get(c);
            if (replacement == null) {
                folded.append(Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD));
            } else {
                folded.append(replacement);
            }
        }
        return COMBINING_MARKS.matcher(folded).replaceAll("");
    }
}
