package de.foodplanner.domain.search;

import java.util.Map;

/**
 * Einträge aus der {@code unaccent.rules} von PostgreSQL, die NFD nicht abdeckt (§5.5): Buchstaben ohne Zerlegung in
 * Grundbuchstabe und Akzent. Schlüssel und Werte sind kleingeschrieben, weil die Faltung zuerst kleinschreibt. Die Einträge,
 * deren Wert dem Schlüssel gleicht (ǣ, ǯ, ǽ, ǿ), sind der umgekehrte Fall: NFD würde sie zerlegen, {@code unaccent}
 * lässt sie unverändert. Ob die
 * Tabelle vollständig ist, prüft {@code SearchFoldingTest} in {@code infrastructure} gegen die Datenbank (§12).
 */
final class UnaccentExceptions {

    static final Map<Character, String> MAP = Map.ofEntries(
            Map.entry('\u00df', "ss"),
            Map.entry('\u00e6', "ae"),
            Map.entry('\u00f0', "d"),
            Map.entry('\u00f8', "o"),
            Map.entry('\u00fe', "th"),
            Map.entry('\u0111', "d"),
            Map.entry('\u0127', "h"),
            Map.entry('\u0131', "i"),
            Map.entry('\u0133', "ij"),
            Map.entry('\u0138', "q"),
            Map.entry('\u0140', "l"),
            Map.entry('\u0142', "l"),
            Map.entry('\u0149', "'n"),
            Map.entry('\u014b', "n"),
            Map.entry('\u0153', "oe"),
            Map.entry('\u0167', "t"),
            Map.entry('\u017f', "s"),
            Map.entry('\u0180', "b"),
            Map.entry('\u0183', "b"),
            Map.entry('\u0188', "c"),
            Map.entry('\u018c', "d"),
            Map.entry('\u0192', "f"),
            Map.entry('\u0195', "hv"),
            Map.entry('\u0199', "k"),
            Map.entry('\u019a', "l"),
            Map.entry('\u019e', "n"),
            Map.entry('\u01a3', "oi"),
            Map.entry('\u01a5', "p"),
            Map.entry('\u01ab', "t"),
            Map.entry('\u01ad', "t"),
            Map.entry('\u01b4', "y"),
            Map.entry('\u01b6', "z"),
            Map.entry('\u01c6', "dz"),
            Map.entry('\u01c9', "lj"),
            Map.entry('\u01cc', "nj"),
            Map.entry('\u01e3', "\u01e3"),
            Map.entry('\u01e5', "g"),
            Map.entry('\u01ef', "\u01ef"),
            Map.entry('\u01f3', "dz"),
            Map.entry('\u01fd', "\u01fd"),
            Map.entry('\u01ff', "\u01ff"),
            Map.entry('\u0221', "d"),
            Map.entry('\u0225', "z"),
            Map.entry('\u0234', "l"),
            Map.entry('\u0235', "n"),
            Map.entry('\u0236', "t"),
            Map.entry('\u0237', "j"),
            Map.entry('\u0238', "db"),
            Map.entry('\u0239', "qp"),
            Map.entry('\u023c', "c"),
            Map.entry('\u023f', "s"),
            Map.entry('\u0240', "z"),
            Map.entry('\u0247', "e"),
            Map.entry('\u0249', "j"),
            Map.entry('\u024d', "r"),
            Map.entry('\u024f', "y"),
            Map.entry('\u0253', "b"),
            Map.entry('\u0256', "d"),
            Map.entry('\u0257', "d"),
            Map.entry('\u025b', "e"),
            Map.entry('\u0260', "g"),
            Map.entry('\u0268', "i"),
            Map.entry('\u0272', "n"),
            Map.entry('\u0288', "t"),
            Map.entry('\u0289', "u"),
            Map.entry('\u028b', "v"),
            Map.entry('\u2c65', "a"),
            Map.entry('\u2c66', "t"));

    private UnaccentExceptions() {
    }
}
