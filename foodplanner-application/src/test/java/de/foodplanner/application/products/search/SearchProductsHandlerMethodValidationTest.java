package de.foodplanner.application.products.search;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import de.foodplanner.application.port.SearchSort;
import jakarta.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchProductsHandlerMethodValidationTest {

    private AnnotationConfigApplicationContext context;
    private FakeProductSearch search;
    private SearchProductsHandler handler;

    @BeforeEach
    void setUp() {
        search = new FakeProductSearch();
        context = new AnnotationConfigApplicationContext();
        context.registerBean(LocalValidatorFactoryBean.class);
        context.registerBean(MethodValidationPostProcessor.class, MethodValidationPostProcessor::new);
        context.registerBean(FakeProductSearch.class, () -> search);
        context.registerBean(SearchProductsHandler.class);
        context.refresh();
        handler = context.getBean(SearchProductsHandler.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void invalidQueryIsRejectedBeforeThePortIsCalled() {
        SearchProductsQuery invalid = new SearchProductsQuery(null, null, null, null, null,
                SearchSort.RELEVANCE, 1, 101);

        assertThatThrownBy(() -> handler.handle(invalid))
                .isInstanceOfSatisfying(ConstraintViolationException.class, e -> assertThat(e.getConstraintViolations())
                        .singleElement()
                        .satisfies(v -> assertThat(v.getPropertyPath().toString()).endsWith("pageSize")));
        assertThat(search.calls).isZero();
    }

    @Test
    void validQueryReachesPort() {
        handler.handle(new SearchProductsQuery("Milch", null, null, null, null, SearchSort.RELEVANCE, 1, 20));

        assertThat(search.calls).isEqualTo(1);
        assertThat(search.method).isEqualTo("text");
    }
}
