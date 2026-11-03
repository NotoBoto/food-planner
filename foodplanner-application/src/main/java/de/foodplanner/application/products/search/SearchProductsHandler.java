package de.foodplanner.application.products.search;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.PageRequest;
import de.foodplanner.application.port.PagedResult;
import de.foodplanner.application.port.ProductSearch;
import de.foodplanner.application.port.ProductSummary;
import de.foodplanner.application.port.SearchFilter;
import de.foodplanner.domain.search.SearchText;
import jakarta.validation.Valid;

/**
 * Produktsuche (§5.1): 8 oder 13 Ziffern sind eine EAN-Suche ohne Filter (§5.4), ein Text ohne Tokens
 * durchsucht alles, sonst gilt die lexikalische Suche (§5.5).
 */
@Service
@Validated
public class SearchProductsHandler {

    private static final Pattern EAN = Pattern.compile("\\d{8}|\\d{13}");

    private final ProductSearch productSearch;

    public SearchProductsHandler(ProductSearch productSearch) {
        this.productSearch = productSearch;
    }

    public PagedResult<ProductSummary> handle(@Valid SearchProductsQuery query) {
        PageRequest page = new PageRequest(query.page(), query.pageSize());
        String raw = query.q() == null ? "" : query.q();
        String trimmed = raw.trim();
        if (EAN.matcher(trimmed).matches()) {
            return productSearch.byOffCode(trimmed, page);
        }
        SearchFilter filter = new SearchFilter(
                query.category(), query.minPriceCents(), query.maxPriceCents(), query.minProtein100g());
        SearchText text = SearchText.parse(raw);
        if (text.isEmpty()) {
            return productSearch.browse(filter, query.sort(), page);
        }
        return productSearch.text(text, filter, query.sort(), page);
    }
}
