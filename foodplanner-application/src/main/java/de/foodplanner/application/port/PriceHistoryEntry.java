package de.foodplanner.application.port;

import java.time.Instant;

/**
 * Ein Preis aus dem Verlauf eines Produkts (§5.4). Ein Preis wird abgelöst, nie überschrieben (§4.4).
 *
 * @param priceCents Packungspreis in Cent
 * @param scrapedAt  Zeitpunkt des Scrapes
 * @param isCurrent  ob dies der aktuelle Preis ist; höchstens einer je Produkt
 */
public record PriceHistoryEntry(long priceCents, Instant scrapedAt, boolean isCurrent) {
}
