package de.foodplanner.application.port;

/**
 * Seitenwunsch (§5.1); {@code page} zählt ab 1.
 *
 * @param page     Seitennummer, ab 1
 * @param pageSize Einträge pro Seite
 */
public record PageRequest(int page, int pageSize) {

    /** Anzahl der zu überspringenden Zeilen. */
    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}
