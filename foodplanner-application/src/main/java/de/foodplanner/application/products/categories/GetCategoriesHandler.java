package de.foodplanner.application.products.categories;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.ProductRepository;

/**
 * Liefert die Kategorien der in der Suche sichtbaren Produkte, ohne Duplikate, ohne {@code NULL}, sortiert
 * (§5.1, Grooming-Entscheidung 11).
 */
@Service
@Validated
public class GetCategoriesHandler {

    private final ProductRepository productRepository;

    public GetCategoriesHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<String> handle(GetCategoriesQuery query) {
        return productRepository.categories();
    }
}
