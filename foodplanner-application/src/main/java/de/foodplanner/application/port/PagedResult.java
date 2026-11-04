package de.foodplanner.application.port;

import java.util.List;

/**
 * Eine Ergebnisseite (§5.1). Ohne Treffer ist {@code totalPages} 0; eine Seite hinter dem Ende hat leere {@code items}.
 *
 * @param items      Einträge der Seite
 * @param page       Seitennummer, ab 1
 * @param pageSize   Einträge pro Seite
 * @param totalCount Gesamtzahl der Treffer
 * @param totalPages {@code ceil(totalCount / pageSize)}
 */
public record PagedResult<T>(List<T> items, int page, int pageSize, long totalCount, int totalPages) {

    public static <T> PagedResult<T> of(List<T> items, PageRequest request, long totalCount) {
        int totalPages = (int) ((totalCount + request.pageSize() - 1) / request.pageSize());
        return new PagedResult<>(items, request.page(), request.pageSize(), totalCount, totalPages);
    }
}
