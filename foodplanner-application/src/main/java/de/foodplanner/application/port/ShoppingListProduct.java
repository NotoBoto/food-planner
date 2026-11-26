package de.foodplanner.application.port;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Read-Model eines Produkts für die Listenzusammenfassung (§5.2): nur, was die Summen brauchen. Anders als
 * {@link ProductDetail} sind die Nährwerte <b>ungerundet</b>, denn sie gehen in die Rechnung ein und werden erst an der
 * API-Grenze gerundet (§4.4). Es gibt nur Produkte mit aktuellem Preis; ein Produkt ohne gilt als nicht im Katalog
 * (§5.2).
 *
 * @param priceCents   aktueller Preis in Cent
 * @param packageGrams Packungsgröße in Gramm, bei {@code unit = ML} in Millilitern
 * @param protein100   Eiweiß pro 100 g bzw. 100 ml; die vier Nährwerte sind alle gesetzt oder alle {@code null}
 */
public record ShoppingListProduct(
        String productKey,
        String name,
        int packageGrams,
        String unit,
        long priceCents,
        @Nullable BigDecimal protein100,
        @Nullable BigDecimal fat100,
        @Nullable BigDecimal carbs100,
        @Nullable BigDecimal kcal100) {
}
