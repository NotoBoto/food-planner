package de.foodplanner.domain.product;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natürlicher Schlüssel eines Produkts über beide Quellen: {@code aldi:<sku>} oder {@code off:<code>}.
 *
 * @param value der Schlüssel in der Form {@code aldi:<sku>} oder {@code off:<code>}
 */
public record ProductKey(String value) {

    private static final String ALDI = "aldi:";
    private static final String OFF = "off:";

    /** Präfix in Kleinbuchstaben, danach ein nichtleerer Rest ohne Leerraum. */
    private static final Pattern FORMAT = Pattern.compile("(aldi|off):(\\S+)");

    /**
     * Die ALDI-SKU hat Vorrang; ohne sie gilt der Open-Food-Facts-Code.
     *
     * @param aldiSku SKU im ALDI-Shop, darf {@code null} sein
     * @param offCode Barcode bei Open Food Facts, darf {@code null} sein
     * @throws IllegalArgumentException wenn beide {@code null} sind
     */
    public static ProductKey of(String aldiSku, String offCode) {
        if (aldiSku != null) {
            return new ProductKey(ALDI + aldiSku);
        }
        if (offCode != null) {
            return new ProductKey(OFF + offCode);
        }
        throw new IllegalArgumentException("aldiSku und offCode sind beide null");
    }

    /**
     * Liest einen gespeicherten Schlüssel, etwa aus einer Einkaufsliste (§5.1). Ein Wert in anderer Form ist kein
     * Fehler, sondern ein unbekannter Schlüssel: Der Sammelabruf lässt ihn aus, statt die ganze Anfrage abzulehnen.
     * Es wird nichts getrimmt; Leerraum irgendwo im Wert macht ihn ungültig.
     *
     * @param raw Schlüssel in der Form {@code aldi:<sku>} oder {@code off:<code>}, darf {@code null} sein
     * @return der Schlüssel, oder leer, wenn {@code raw} diese Form nicht hat
     */
    public static Optional<ProductKey> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMAT.matcher(raw);
        return matcher.matches() ? Optional.of(new ProductKey(raw)) : Optional.empty();
    }

    /** Die ALDI-SKU, wenn dies ein {@code aldi:}-Schlüssel ist. */
    public Optional<String> aldiSku() {
        return value.startsWith(ALDI) ? Optional.of(value.substring(ALDI.length())) : Optional.empty();
    }

    /** Der Open-Food-Facts-Code, wenn dies ein {@code off:}-Schlüssel ist. */
    public Optional<String> offCode() {
        return value.startsWith(OFF) ? Optional.of(value.substring(OFF.length())) : Optional.empty();
    }

    @Override
    public String toString() {
        return value;
    }
}
