package de.foodplanner.application.port;

import java.util.List;

/** Lesender Zugriff auf den Produktkatalog (§3.1). */
public interface ProductRepository {

    /** Zählt die in der Suche sichtbaren Produkte und liefert das jüngste {@code scrapedAt} ihrer aktuellen Preise. */
    CatalogInfo catalogInfo();

    /** Liefert die verschiedenen Kategorien der in der Suche sichtbaren Produkte, ohne {@code NULL}, sortiert. */
    List<String> categories();
}
