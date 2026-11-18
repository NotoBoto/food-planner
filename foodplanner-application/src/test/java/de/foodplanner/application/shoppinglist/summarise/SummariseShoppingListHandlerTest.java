package de.foodplanner.application.shoppinglist.summarise;

import static de.foodplanner.application.products.FakeProductRepository.listed;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.port.ShoppingListProduct;
import de.foodplanner.application.products.FakeProductRepository;
import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Goals;
import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Item;
import de.foodplanner.domain.product.ProductKey;
import de.foodplanner.domain.shoppinglist.GoalProgress;
import de.foodplanner.domain.shoppinglist.Nutrition;

class SummariseShoppingListHandlerTest {

    private static final Instant ADDED = Instant.parse("2026-11-20T10:00:00Z");

    private static Item item(String key, int quantity, long priceAtAddCents) {
        return new Item(key, quantity, priceAtAddCents, ADDED);
    }

    private static SummariseShoppingListQuery query(Item... items) {
        return new SummariseShoppingListQuery(List.of(items), null);
    }

    @Test
    void linesInRequestOrderDespitePortOrder() {
        FakeProductRepository repository = new FakeProductRepository()
                .withShoppingListProducts(listed("aldi:1", 100), listed("aldi:2", 200), listed("aldi:3", 300));

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository)
                .handle(query(item("aldi:2", 1, 200), item("aldi:3", 1, 300), item("aldi:1", 1, 100)));

        assertThat(summary.lines()).extracting(ShoppingListSummary.Line::productKey)
                .containsExactly("aldi:2", "aldi:3", "aldi:1");
    }

    @Test
    void unknownMalformedAndPricelessKeysAreMissingInRequestOrder() {
        // off:7 steht für ein Produkt ohne aktuellen Preis: Der Port liefert es nicht, wie der echte (§5.2).
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(listed("aldi:1", 100));

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository).handle(query(
                item("aldi:9", 1, 100), item("foo", 1, 100), item("aldi:1", 1, 100), item("off:7", 1, 100)));

        assertThat(summary.lines()).extracting(ShoppingListSummary.Line::productKey).containsExactly("aldi:1");
        assertThat(summary.missingProductKeys()).containsExactly("aldi:9", "foo", "off:7");
    }

    @Test
    void onePortCallWithParsedKeysOnly() {
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(listed("aldi:1", 100));

        new SummariseShoppingListHandler(repository)
                .handle(query(item("aldi:1", 1, 100), item("foo", 1, 100), item("off:7", 1, 100)));

        assertThat(repository.shoppingListProductsCalls).singleElement().satisfies(keys -> assertThat(keys)
                .extracting(ProductKey::value).containsExactly("aldi:1", "off:7"));
    }

    @Test
    void emptyItemsDoNotCallPort() {
        FakeProductRepository repository = new FakeProductRepository();

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository).handle(query());

        assertThat(summary.lines()).isEmpty();
        assertThat(summary.missingProductKeys()).isEmpty();
        assertThat(summary.totals().priceCents()).isZero();
        assertThat(summary.totals().nutrition()).isEqualTo(Nutrition.ZERO);
        assertThat(repository.shoppingListProductsCalls).isEmpty();
    }

    @Test
    void onlyMalformedKeysDoNotCallPort() {
        FakeProductRepository repository = new FakeProductRepository();

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository).handle(query(item("foo", 1, 1)));

        assertThat(summary.missingProductKeys()).containsExactly("foo");
        assertThat(repository.shoppingListProductsCalls).isEmpty();
    }

    @Test
    void lineCarriesRequestAndProductFields() {
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(listed("aldi:1", 139));

        ShoppingListSummary.Line line = new SummariseShoppingListHandler(repository)
                .handle(query(item("aldi:1", 3, 129))).lines().getFirst();

        assertThat(line.name()).isEqualTo("Produkt aldi:1");
        assertThat(line.unit()).isEqualTo("G");
        assertThat(line.addedAt()).isEqualTo(ADDED);
        assertThat(line.line().quantity()).isEqualTo(3);
        assertThat(line.line().unitPriceCents()).isEqualTo(139);
        assertThat(line.line().priceAtAddCents()).isEqualTo(129);
        assertThat(line.line().packageGrams()).isEqualTo(1000);
        assertThat(line.line().per100g().protein()).isEqualByComparingTo("1");
        assertThat(line.line().per100g().kcal()).isEqualByComparingTo("4");
    }

    @Test
    void totalsOverAllLines() {
        FakeProductRepository repository = new FakeProductRepository()
                .withShoppingListProducts(listed("aldi:1", 139), listed("aldi:2", 189));

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository)
                .handle(query(item("aldi:1", 3, 129), item("aldi:2", 1, 199)));

        assertThat(summary.totals().priceCents()).isEqualTo(3 * 139 + 189);
        assertThat(summary.totals().priceDifferenceCents()).isEqualTo(30 - 10);
        assertThat(summary.totals().nutrition().protein()).isEqualByComparingTo("40");
    }

    @Test
    void productWithoutNutritionHasNullPer100g() {
        ShoppingListProduct bread = new ShoppingListProduct("aldi:1", "Brot", 750, "G", 189, null, null, null, null);
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(bread);

        ShoppingListSummary summary = new SummariseShoppingListHandler(repository).handle(query(item("aldi:1", 1, 1)));

        assertThat(summary.lines().getFirst().line().per100g()).isNull();
        assertThat(summary.lines().getFirst().line().nutrition()).isNull();
    }

    @Test
    void goalsNullGivesNullProgress() {
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(listed("aldi:1", 100));

        assertThat(new SummariseShoppingListHandler(repository).handle(query(item("aldi:1", 1, 100))).goalProgress())
                .isNull();
    }

    @Test
    void goalsGiveProgress() {
        FakeProductRepository repository = new FakeProductRepository().withShoppingListProducts(listed("aldi:1", 100));
        SummariseShoppingListQuery query = new SummariseShoppingListQuery(List.of(item("aldi:1", 2, 100)),
                new Goals(40L, null, 120L, null, 400L));

        // 2 × 1000 g × 1 g/100 g = 20 g Eiweiß, 60 g Kohlenhydrate, 200 Cent.
        assertThat(new SummariseShoppingListHandler(repository).handle(query).goalProgress())
                .isEqualTo(new GoalProgress(50L, null, 50L, null, 50L));
    }
}
