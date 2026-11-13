package de.foodplanner.application.products.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import de.foodplanner.application.products.FakeProductRepository;
import jakarta.validation.ConstraintViolationException;

class GetProductsBatchHandlerMethodValidationTest {

    private AnnotationConfigApplicationContext context;
    private FakeProductRepository repository;
    private GetProductsBatchHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository(FakeProductRepository.product(1, "aldi:1001"));
        context = new AnnotationConfigApplicationContext();
        context.registerBean(LocalValidatorFactoryBean.class);
        context.registerBean(MethodValidationPostProcessor.class, MethodValidationPostProcessor::new);
        context.registerBean(FakeProductRepository.class, () -> repository);
        context.registerBean(GetProductsBatchHandler.class);
        context.refresh();
        handler = context.getBean(GetProductsBatchHandler.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void moreThan200KeysAreRejectedBeforeThePortIsCalled() {
        GetProductsBatchQuery tooMany = new GetProductsBatchQuery(Collections.nCopies(201, "aldi:1001"));

        assertThatThrownBy(() -> handler.handle(tooMany))
                .isInstanceOfSatisfying(ConstraintViolationException.class, e -> assertThat(e.getConstraintViolations())
                        .singleElement()
                        .satisfies(v -> assertThat(v.getPropertyPath().toString()).endsWith("keys")));
        assertThat(repository.detailsCalls).isEmpty();
    }

    @Test
    void validQueryReachesPort() {
        assertThat(handler.handle(new GetProductsBatchQuery(Collections.nCopies(200, "aldi:1001")))).hasSize(1);
        assertThat(repository.detailsCalls).hasSize(1);
    }
}
