package de.foodplanner.api.products;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * JSON-Body von {@code POST /api/v1/products/batch}: {@code { "keys": ["aldi:1234567", "off:4008400401211"] }}
 * (§5.1). Der API-Vertrag bleibt so vom Query-Record der Anwendung getrennt; geprüft wird erst im Handler (§3.2).
 *
 * @param keys gespeicherte {@code productKey}s; fehlt das Feld, meldet der Handler {@code errors.keys}
 */
record ProductBatchRequest(@Nullable List<@Nullable String> keys) {
}
