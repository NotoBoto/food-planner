package de.foodplanner.application.products.getbyid;

import static de.foodplanner.application.products.FakeProductRepository.product;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.products.FakeProductRepository;

class GetProductByIdHandlerTest {

    private final ProductDetail milk = product(42, "aldi:1001");
    private final GetProductByIdHandler handler = new GetProductByIdHandler(new FakeProductRepository(milk));

    @Test
    void knownIdReturnsDetail() {
        assertThat(handler.handle(new GetProductByIdQuery(42))).contains(milk);
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(handler.handle(new GetProductByIdQuery(43))).isEmpty();
    }
}
