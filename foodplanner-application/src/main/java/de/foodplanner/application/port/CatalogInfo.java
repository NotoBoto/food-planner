package de.foodplanner.application.port;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Größe und Aktualität des Katalogs (§5.1).
 *
 * @param productCount  Anzahl der in der Suche sichtbaren Produkte
 * @param lastScrapedAt größtes {@code scrapedAt} der aktuellen Preise dieser Produkte; {@code null} bei leerem Katalog
 */
public record CatalogInfo(long productCount, @Nullable Instant lastScrapedAt) {
}
