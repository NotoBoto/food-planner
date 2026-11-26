package de.foodplanner.application.products.batch;

import static de.foodplanner.application.products.FakeProductRepository.product;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.products.FakeProductRepository;
import de.foodplanner.domain.product.ProductKey;

class GetProductsBatchHandlerTest {

    private final ProductDetail milk = product(1, "aldi:1001");
    private final ProductDetail bread = product(2, "off:4008400401211");
    private final ProductDetail cheese = product(3, "aldi:1003");
    private final FakeProductRepository repository = new FakeProductRepository(milk, bread, cheese);
    private final GetProductsBatchHandler handler = new GetProductsBatchHandler(repository);

    private List<ProductDetail> handle(String... keys) {
        return handler.handle(new GetProductsBatchQuery(Arrays.asList(keys)));
    }

    @Test
    void aldiAndOffKeys() {
        assertThat(handle("aldi:1001", "off:4008400401211")).containsExactly(milk, bread);
    }

    @Test
    void unknownKeyIsMissingWithoutError() {
        assertThat(handle("aldi:1001", "aldi:9999", "off:1234")).containsExactly(milk);
    }

    @Test
    void orderIsFirstAppearanceInRequest() {
        assertThat(handle("aldi:1003", "aldi:1001", "off:4008400401211")).containsExactly(cheese, milk, bread);
    }

    @Test
    void duplicateKeysYieldEachProductOnce() {
        assertThat(handle("aldi:1001", "aldi:1003", "aldi:1001")).containsExactly(milk, cheese);
        assertThat(repository.detailsCalls.getLast())
                .containsExactly(ProductKey.of("1001", null), ProductKey.of("1003", null));
    }

    @Test
    void malformedAndNullKeysAreSkippedAsUnknown() {
        assertThat(handle("foo", null, "aldi:", " aldi:1001", "aldi:1003")).containsExactly(cheese);
        assertThat(repository.detailsCalls.getLast()).containsExactly(ProductKey.of("1003", null));
    }

    @Test
    void noUsableKeyDoesNotCallThePort() {
        assertThat(handle()).isEmpty();
        assertThat(handle("foo", null)).isEmpty();
        assertThat(repository.detailsCalls).isEmpty();
    }
}
