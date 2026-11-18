package de.foodplanner.domain.product;

/**
 * Kilopreis in Cent. Geld bleibt im Preispfad ganzzahlig, weder {@code double} noch {@code BigDecimal} (§4.4).
 */
public final class PricePerKg {

    private PricePerKg() {
    }

    /**
     * Rechnet {@code priceCents × 1000 / packageGrams}, gerundet {@code HALF_UP}, rein ganzzahlig. Der Bruch wird mit
     * 2 erweitert und der halbe Nenner addiert; {@code floorDiv} rundet dann kaufmännisch auf.
     *
     * @param priceCents   Packungspreis in Cent
     * @param packageGrams Packungsgröße in Gramm, größer als 0
     */
    public static long cents(long priceCents, int packageGrams) {
        return Math.floorDiv(2000 * priceCents + packageGrams, 2L * packageGrams);
    }
}
