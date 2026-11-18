package de.foodplanner.application.shoppinglist.summarise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import de.foodplanner.application.products.FakeProductRepository;
import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Item;
import jakarta.validation.ConstraintViolationException;

class SummariseShoppingListHandlerMethodValidationTest {

    private AnnotationConfigApplicationContext context;
    private FakeProductRepository repository;
    private SummariseShoppingListHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository().withShoppingListProducts(FakeProductRepository.listed("aldi:1", 100));
        context = new AnnotationConfigApplicationContext();
        context.registerBean(LocalValidatorFactoryBean.class);
        context.registerBean(MethodValidationPostProcessor.class, MethodValidationPostProcessor::new);
        context.registerBean(FakeProductRepository.class, () -> repository);
        context.registerBean(SummariseShoppingListHandler.class);
        context.refresh();
        handler = context.getBean(SummariseShoppingListHandler.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static List<Item> items(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new Item("aldi:" + i, 1, 100L, Instant.parse("2026-11-20T10:00:00Z")))
                .toList();
    }

    @Test
    void moreThan200ItemsAreRejectedBeforeThePortIsCalled() {
        SummariseShoppingListQuery tooMany = new SummariseShoppingListQuery(items(201), null);

        assertThatThrownBy(() -> handler.handle(tooMany))
                .isInstanceOfSatisfying(ConstraintViolationException.class, e -> assertThat(e.getConstraintViolations())
                        .singleElement()
                        .satisfies(v -> assertThat(v.getPropertyPath().toString()).endsWith("items")));
        assertThat(repository.shoppingListProductsCalls).isEmpty();
    }

    @Test
    void validQueryReachesPort() {
        assertThat(handler.handle(new SummariseShoppingListQuery(items(200), null)).lines()).hasSize(1);
        assertThat(repository.shoppingListProductsCalls).hasSize(1);
    }
}
