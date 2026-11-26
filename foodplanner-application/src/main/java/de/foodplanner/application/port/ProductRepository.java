package de.foodplanner.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.foodplanner.domain.product.ProductKey;

/** Lesender Zugriff auf den Produktkatalog (§3.1). */
public interface ProductRepository {

    /** Zählt die in der Suche sichtbaren Produkte und liefert das jüngste {@code scrapedAt} ihrer aktuellen Preise. */
    CatalogInfo catalogInfo();

    /** Liefert die verschiedenen Kategorien der in der Suche sichtbaren Produkte, ohne {@code NULL}, sortiert. */
    List<String> categories();

    /**
     * Ein Produkt über seine interne ID, mit oder ohne aktuellen Preis (§5.1).
     *
     * @return das Produkt, oder leer, wenn es die ID nicht gibt
     */
    Optional<ProductDetail> detail(long id);

    /**
     * Die Produkte zu den Schlüsseln, mit oder ohne aktuellen Preis (§5.1). Ein Schlüssel trifft ein Produkt nur, wenn
     * es genau diesen {@code productKey} hat: {@code off:<code>} findet also kein Produkt mit Artikelnummer, denn
     * dessen Schlüssel ist {@code aldi:<sku>} (§4.2). Unbekannte Schlüssel fehlen im Ergebnis.
     *
     * @return jedes getroffene Produkt genau einmal, in keiner bestimmten Reihenfolge
     */
    List<ProductDetail> details(Collection<ProductKey> keys);

    /**
     * Die Produkte zu den Schlüsseln für die Listenzusammenfassung (§5.2): nur Produkte mit aktuellem Preis, wie in
     * der Suche; ein Produkt ohne gilt als nicht im Katalog. Nährwerte ungerundet. Schlüsselabgleich wie bei
     * {@link #details}; unbekannte Schlüssel fehlen im Ergebnis.
     *
     * @return jedes getroffene Produkt genau einmal, in keiner bestimmten Reihenfolge
     */
    List<ShoppingListProduct> shoppingListProducts(Collection<ProductKey> keys);
}
