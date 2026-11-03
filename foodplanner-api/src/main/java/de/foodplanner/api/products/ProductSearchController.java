package de.foodplanner.api.products;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.foodplanner.application.port.PagedResult;
import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.application.port.SearchSort;
import de.foodplanner.application.products.search.SearchProductsHandler;
import de.foodplanner.application.products.search.SearchProductsQuery;

/**
 * {@code GET /api/v1/products/search → 200 PagedResult<ProductSummary>} (§5.1). Der Controller bindet nur
 * die Parameter; die Grenzen prüft der {@code @Validated}-Handler (§3.2).
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductSearchController {

    private final SearchProductsHandler handler;

    ProductSearchController(SearchProductsHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/search")
    PagedResult<ProductSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minPriceCents,
            @RequestParam(required = false) Long maxPriceCents,
            @RequestParam(required = false) BigDecimal minProtein100g,
            @RequestParam(defaultValue = "relevance") SearchSort sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return handler.handle(new SearchProductsQuery(
                q, category, minPriceCents, maxPriceCents, minProtein100g, sort, page, pageSize));
    }
}
