package de.foodplanner.application.catalog.info;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.CatalogInfo;
import de.foodplanner.application.port.ProductRepository;

/** Liefert {@code productCount} und {@code lastScrapedAt} der in der Suche sichtbaren Produkte (§5.1). */
@Service
@Validated
public class GetCatalogInfoHandler {

    private final ProductRepository productRepository;

    public GetCatalogInfoHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CatalogInfo handle(GetCatalogInfoQuery query) {
        return productRepository.catalogInfo();
    }
}
