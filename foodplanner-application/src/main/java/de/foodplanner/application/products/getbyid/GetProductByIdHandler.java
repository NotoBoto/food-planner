package de.foodplanner.application.products.getbyid;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.port.ProductRepository;

/**
 * Ein Produkt mit allen Details über seine interne ID (§5.1). Kein Filter auf einen aktuellen Preis: Eine Detailseite
 * bleibt erreichbar, auch wenn das Produkt gerade keinen Preis hat.
 */
@Service
@Validated
public class GetProductByIdHandler {

    private final ProductRepository productRepository;

    public GetProductByIdHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** @return das Produkt, oder leer, wenn es die ID nicht gibt; was daraus wird, entscheidet der Aufrufer */
    public Optional<ProductDetail> handle(GetProductByIdQuery query) {
        return productRepository.detail(query.id());
    }
}
