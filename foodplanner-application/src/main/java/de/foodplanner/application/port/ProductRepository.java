package de.foodplanner.application.port;

/** Lesender Zugriff auf den Produktkatalog (§3.1). */
public interface ProductRepository {

    /** Zählt die in der Suche sichtbaren Produkte und liefert das jüngste {@code scrapedAt} ihrer aktuellen Preise. */
    CatalogInfo catalogInfo();
}
