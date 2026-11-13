package de.foodplanner.api.products;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.products.batch.GetProductsBatchHandler;
import de.foodplanner.application.products.batch.GetProductsBatchQuery;
import de.foodplanner.application.products.getbyid.GetProductByIdHandler;
import de.foodplanner.application.products.getbyid.GetProductByIdQuery;

/**
 * {@code GET /api/v1/products/{id} → 200 ProductDetail | 404} und
 * {@code POST /api/v1/products/batch → 200 ProductDetail[]} (§5.1). Der Controller bindet nur; die Grenze von 200
 * Schlüsseln prüft der {@code @Validated}-Handler (§3.2).
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductDetailController {

    private final GetProductByIdHandler getById;
    private final GetProductsBatchHandler batch;

    ProductDetailController(GetProductByIdHandler getById, GetProductsBatchHandler batch) {
        this.getById = getById;
        this.batch = batch;
    }

    /** Nur Ziffern: {@code GET /batch} trifft so keinen Detailpfad, sondern endet als 405 (falsche Methode). */
    @GetMapping("/{id:\\d+}")
    ProductDetail detail(@PathVariable long id) {
        return getById.handle(new GetProductByIdQuery(id)).orElseThrow(() -> notFound(id));
    }

    @PostMapping("/batch")
    List<ProductDetail> batch(@RequestBody ProductBatchRequest request) {
        // Keine Prüfung hier: Ein fehlendes keys wird zu null und vom @Validated-Handler gemeldet.
        return batch.handle(new GetProductsBatchQuery(request.keys()));
    }

    /** {@code title} und {@code detail} zeigt das Frontend direkt an (§7.4); das Format vereinheitlicht #25. */
    private static ErrorResponseException notFound(long id) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Es gibt kein Produkt mit der ID " + id + ".");
        problem.setTitle("Produkt nicht gefunden");
        return new ErrorResponseException(HttpStatus.NOT_FOUND, problem, null);
    }
}
