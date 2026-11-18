package de.foodplanner.domain.shoppinglist;

/**
 * Eine Zeile der Einkaufsliste mit dem aktuellen Preis ihres Produkts (§5.2). Geld bleibt ganzzahlig in Cent, weder
 * {@code double} noch {@code BigDecimal} (§4.4); Nährwerte bleiben ungerundet.
 *
 * <p>„{@code quantity} in einer Listenzeile bedeutet ganze Packungen, nicht Gramm. Nährwerte sind
 * {@code quantity × packageGrams × per100g / 100}." (§4.4) Bei {@code unit = ML} ist {@code packageGrams} die Menge
 * in Millilitern und {@code per100g} gilt pro 100 ml; die Formel bleibt dieselbe, denn Einheiten gehen nie in die
 * Arithmetik ein (§4.4, §6.1).
 *
 * <p><b>Überlauf.</b> Die Anfrage begrenzt {@code quantity} auf 1…1000 und {@code priceAtAddCents} auf 1…100 000;
 * {@code unitPriceCents} kommt dagegen aus der Datenbank und ist dort nur nach unten begrenzt. Die Rechnung nutzt
 * deshalb {@link Math#multiplyExact} und {@link Math#subtractExact}: Ein unrealistisch hoher Preis endet als
 * {@link ArithmeticException}, nie als still übergelaufene Summe.
 *
 * @param quantity        Anzahl Packungen, mindestens 1
 * @param unitPriceCents  aktueller Packungspreis in Cent
 * @param priceAtAddCents Packungspreis beim Hinzufügen zur Liste, in Cent (§4.6)
 * @param packageGrams    Packungsgröße in Gramm bzw. Millilitern, mindestens 1
 * @param per100g         Nährwerte pro 100 g bzw. 100 ml, oder {@code null}, wenn das Produkt keine hat
 */
public record ShoppingLine(int quantity, long unitPriceCents, long priceAtAddCents, int packageGrams,
        Nutrition per100g) {

    /** {@code quantity × unitPriceCents}. */
    public long lineTotalCents() {
        return Math.multiplyExact(quantity, unitPriceCents);
    }

    /** {@code unitPriceCents − priceAtAddCents} (§5.2); negativ, wenn das Produkt günstiger geworden ist. */
    public long priceChangeCents() {
        return Math.subtractExact(unitPriceCents, priceAtAddCents);
    }

    /** {@code priceChangeCents × quantity}: der Beitrag der Zeile zu {@code priceDifferenceCents} (§5.2). */
    public long priceDifferenceCents() {
        return Math.multiplyExact(priceChangeCents(), quantity);
    }

    /**
     * {@code quantity × packageGrams × per100g / 100}, ungerundet.
     *
     * @return die Nährwerte der Zeile, oder {@code null}, wenn das Produkt keine hat
     */
    public Nutrition nutrition() {
        return per100g == null ? null : per100g.timesOver100((long) quantity * packageGrams);
    }
}
