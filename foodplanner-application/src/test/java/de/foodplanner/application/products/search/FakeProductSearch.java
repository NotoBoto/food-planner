package de.foodplanner.application.products.search;

import java.util.List;

import de.foodplanner.application.port.PageRequest;
import de.foodplanner.application.port.PagedResult;
import de.foodplanner.application.port.ProductSearch;
import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.application.port.SearchFilter;
import de.foodplanner.application.port.SearchSort;
import de.foodplanner.domain.search.SearchText;

/** Handgeschriebenes Fake: hält den letzten Aufruf fest und liefert eine leere Seite. */
class FakeProductSearch implements ProductSearch {

    String method;
    String offCode;
    SearchText text;
    SearchFilter filter;
    SearchSort sort;
    PageRequest page;
    int calls;

    @Override
    public PagedResult<ProductSummary> byOffCode(String offCode, PageRequest page) {
        record("byOffCode", offCode, null, null, null, page);
        return empty(page);
    }

    @Override
    public PagedResult<ProductSummary> browse(SearchFilter filter, SearchSort sort, PageRequest page) {
        record("browse", null, null, filter, sort, page);
        return empty(page);
    }

    @Override
    public PagedResult<ProductSummary> text(SearchText text, SearchFilter filter, SearchSort sort, PageRequest page) {
        record("text", null, text, filter, sort, page);
        return empty(page);
    }

    private void record(String method, String offCode, SearchText text, SearchFilter filter, SearchSort sort,
            PageRequest page) {
        this.method = method;
        this.offCode = offCode;
        this.text = text;
        this.filter = filter;
        this.sort = sort;
        this.page = page;
        calls++;
    }

    private static PagedResult<ProductSummary> empty(PageRequest page) {
        return PagedResult.of(List.of(), page, 0);
    }
}
