package de.foodplanner.application.products.getbyid;

/**
 * Eingabe der Detailansicht (§5.1).
 *
 * @param id interne Datenbank-ID; gilt nur innerhalb einer laufenden Instanz, gespeichert wird der {@code productKey}
 */
public record GetProductByIdQuery(long id) {
}
