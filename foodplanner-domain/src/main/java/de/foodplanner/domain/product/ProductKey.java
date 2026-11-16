package de.foodplanner.domain.product;

/**
 * Natürlicher Schlüssel eines Produkts über beide Quellen: {@code aldi:<sku>} oder {@code off:<code>}.
 *
 * @param value der Schlüssel in der Form {@code aldi:<sku>} oder {@code off:<code>}
 */
public record ProductKey(String value) {

    /**
     * Die ALDI-SKU hat Vorrang; ohne sie gilt der Open-Food-Facts-Code.
     *
     * @param aldiSku SKU im ALDI-Shop, darf {@code null} sein
     * @param offCode Barcode bei Open Food Facts, darf {@code null} sein
     * @throws IllegalArgumentException wenn beide {@code null} sind
     */
    public static ProductKey of(String aldiSku, String offCode) {
        if (aldiSku != null) {
            return new ProductKey("aldi:" + aldiSku);
        }
        if (offCode != null) {
            return new ProductKey("off:" + offCode);
        }
        throw new IllegalArgumentException("aldiSku und offCode sind beide null");
    }

    @Override
    public String toString() {
        return value;
    }
}
