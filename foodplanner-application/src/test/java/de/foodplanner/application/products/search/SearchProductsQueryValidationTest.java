package de.foodplanner.application.products.search;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.foodplanner.application.port.SearchSort;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

class SearchProductsQueryValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<SearchProductsQuery>> validate(Long min, Long max, SearchSort sort, int page,
            int pageSize) {
        return validator.validate(new SearchProductsQuery(null, null, min, max, null, sort, page, pageSize));
    }

    private void assertViolation(Set<ConstraintViolation<SearchProductsQuery>> violations, String path,
            String message) {
        assertThat(violations).hasSize(1);
        ConstraintViolation<SearchProductsQuery> v = violations.iterator().next();
        assertThat(v.getPropertyPath().toString()).isEqualTo(path);
        assertThat(v.getMessage()).isEqualTo(message);
    }

    @Test
    void validQueryHasNoViolations() {
        assertThat(validate(null, null, SearchSort.RELEVANCE, 1, 20)).isEmpty();
    }

    @Test
    void pageBounds() {
        assertViolation(validate(null, null, SearchSort.RELEVANCE, 0, 20), "page",
                "muss zwischen 1 und 100000 liegen");
        assertViolation(validate(null, null, SearchSort.RELEVANCE, 100001, 20), "page",
                "muss zwischen 1 und 100000 liegen");
        assertThat(validate(null, null, SearchSort.RELEVANCE, 1, 20)).isEmpty();
        assertThat(validate(null, null, SearchSort.RELEVANCE, 100000, 20)).isEmpty();
    }

    @Test
    void pageSizeBounds() {
        assertViolation(validate(null, null, SearchSort.RELEVANCE, 1, 0), "pageSize",
                "muss zwischen 1 und 100 liegen");
        assertViolation(validate(null, null, SearchSort.RELEVANCE, 1, 101), "pageSize",
                "muss zwischen 1 und 100 liegen");
        assertThat(validate(null, null, SearchSort.RELEVANCE, 1, 1)).isEmpty();
        assertThat(validate(null, null, SearchSort.RELEVANCE, 1, 100)).isEmpty();
    }

    @Test
    void sortIsRequired() {
        assertViolation(validate(null, null, null, 1, 20), "sort", "darf nicht leer sein");
    }

    @Test
    void minAboveMaxIsReportedOnMinPriceCents() {
        assertViolation(validate(500L, 400L, SearchSort.RELEVANCE, 1, 20), "minPriceCents",
                "darf nicht größer als maxPriceCents sein");
    }

    @Test
    void equalOrOpenPriceRangeIsValid() {
        assertThat(validate(400L, 400L, SearchSort.RELEVANCE, 1, 20)).isEmpty();
        assertThat(validate(null, 400L, SearchSort.RELEVANCE, 1, 20)).isEmpty();
        assertThat(validate(500L, null, SearchSort.RELEVANCE, 1, 20)).isEmpty();
    }

    @Test
    void qIsLimitedTo200Characters() {
        SearchProductsQuery atLimit = new SearchProductsQuery("a".repeat(200), null, null, null, null,
                SearchSort.RELEVANCE, 1, 20);
        SearchProductsQuery tooLong = new SearchProductsQuery("a".repeat(201), null, null, null, null,
                SearchSort.RELEVANCE, 1, 20);

        assertThat(validator.validate(atLimit)).isEmpty();
        assertViolation(validator.validate(tooLong), "q", "darf höchstens 200 Zeichen lang sein");
    }
}
