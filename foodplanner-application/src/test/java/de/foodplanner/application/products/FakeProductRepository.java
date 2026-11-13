package de.foodplanner.application.products;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.foodplanner.application.port.CatalogInfo;
import de.foodplanner.application.port.ProductDetail;
import de.foodplanner.application.port.ProductRepository;
import de.foodplanner.domain.product.ProductKey;

/**
 * Handgeschriebenes Fake: kennt eine feste Menge Produkte und hält die Aufrufe fest. {@link #details} liefert die
 * Treffer in umgekehrter Reihenfolge, damit Tests sehen, dass die Reihenfolge der Antwort nicht vom Port kommt.
 */
public class FakeProductRepository implements ProductRepository {

    private final List<ProductDetail> products;
    public final List<List<ProductKey>> detailsCalls = new ArrayList<>();

    public FakeProductRepository(ProductDetail... products) {
        this.products = List.of(products);
    }

    /** Ein Produkt mit ID und Schlüssel; die übrigen Felder sind für die Use Cases ohne Bedeutung. */
    public static ProductDetail product(long id, String productKey) {
        return new ProductDetail(id, productKey, "Produkt " + id, null, null, 500, "G", 199L, 398L,
                null, null, null, null, null, null, null, null, List.of());
    }

    @Override
    public Optional<ProductDetail> detail(long id) {
        return products.stream().filter(p -> p.id() == id).findFirst();
    }

    @Override
    public List<ProductDetail> details(Collection<ProductKey> keys) {
        detailsCalls.add(List.copyOf(keys));
        List<String> wanted = keys.stream().map(ProductKey::value).toList();
        return products.stream().filter(p -> wanted.contains(p.productKey())).toList().reversed();
    }

    @Override
    public CatalogInfo catalogInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> categories() {
        throw new UnsupportedOperationException();
    }
}
