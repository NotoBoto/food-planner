package de.foodplanner.application.port;

import de.foodplanner.domain.search.SearchText;

/** Lesender Port der Produktsuche; liefert nur in der Suche sichtbare Produkte (§5.1, §5.4). */
public interface ProductSearch {

    /** EAN-Suche; wendet nur die Sichtbarkeitsregel an, Filter bleiben unberücksichtigt (§5.4). */
    PagedResult<ProductSummary> byOffCode(String offCode, PageRequest page);

    /** Suche ohne Text: alle sichtbaren Produkte, {@code RELEVANCE} bedeutet Sortierung nach Name (§5.5). */
    PagedResult<ProductSummary> browse(SearchFilter filter, SearchSort sort, PageRequest page);

    /**
     * Lexikalische Suche über die besten Treffer nach Score; die Sortierung ordnet diese Menge um (§5.5).
     *
     * @throws IllegalArgumentException wenn {@code text} keine Tokens hat; leere Anfragen gehen an {@link #browse}
     */
    PagedResult<ProductSummary> text(SearchText text, SearchFilter filter, SearchSort sort, PageRequest page);
}
