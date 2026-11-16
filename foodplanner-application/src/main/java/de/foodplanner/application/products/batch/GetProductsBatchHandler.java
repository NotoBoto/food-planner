package de.foodplanner.application.products.batch;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.port.ProductRepository;
import de.foodplanner.domain.product.ProductKey;
import jakarta.validation.Valid;

/**
 * Sammelabruf über {@code productKey} (§5.1): Damit lädt die Browser-App eine gespeicherte Einkaufsliste nach. Eine
 * reine Funktion der Anfrage ohne Zustand (§4.6) und ohne Filter auf einen aktuellen Preis, sonst bräche eine
 * gespeicherte Liste, sobald sich ein Produkt darin ändert.
 *
 * <p>Unbekannte und falsch geformte Schlüssel fehlen in der Antwort, ohne Fehler. Jedes Produkt erscheint genau
 * einmal, in der Reihenfolge, in der sein Schlüssel zuerst in der Anfrage steht.
 */
@Service
@Validated
public class GetProductsBatchHandler {

    private final ProductRepository productRepository;

    public GetProductsBatchHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDetail> handle(@Valid GetProductsBatchQuery query) {
        Set<ProductKey> keys = new LinkedHashSet<>();
        for (String raw : query.keys()) {
            ProductKey.parse(raw).ifPresent(keys::add);
        }
        if (keys.isEmpty()) {
            return List.of();
        }
        Map<String, ProductDetail> byKey = new HashMap<>();
        for (ProductDetail detail : productRepository.details(keys)) {
            byKey.put(detail.productKey(), detail);
        }
        return keys.stream().map(key -> byKey.get(key.value())).filter(Objects::nonNull).toList();
    }
}
