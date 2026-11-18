package de.foodplanner.infrastructure.product;

/**
 * Die Sichtbarkeitsregel der öffentlichen Suche als SQL-Fragment, an genau einer Stelle: „In öffentlichen
 * Suchergebnissen erscheinen nur Produkte mit mindestens einem aktuellen {@code Price}." (§4.3)
 *
 * <p>Das Fragment ist eine {@code FROM}-Klausel mit den Aliasen {@code p} (Produkt) und {@code cp} (sein aktueller
 * Preis). Wegen {@code price_one_current_per_product} liefert es jedes sichtbare Produkt genau einmal. Ausblenden
 * (§10.2) und Auslistung (§10.5) ergänzen ihre Bedingungen in diesem {@code ON}, damit Suche (#19) und
 * {@code catalog/info} (#23) gemeinsam wechseln. Package-private: der Suchadapter liegt deshalb ebenfalls in diesem
 * Paket.
 */
final class VisibleProducts {

    static final String FROM = "product p JOIN price cp ON cp.product_id = p.id AND cp.is_current";

    private VisibleProducts() {
    }
}
