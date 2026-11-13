package de.foodplanner.application.products.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class GetProductsBatchQueryValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<GetProductsBatchQuery>> validate(List<String> keys) {
        return validator.validate(new GetProductsBatchQuery(keys));
    }

    private static void assertViolation(Set<ConstraintViolation<GetProductsBatchQuery>> violations, String message) {
        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.getPropertyPath().toString()).isEqualTo("keys");
            assertThat(v.getMessage()).isEqualTo(message);
        });
    }

    @Test
    void atMost200Keys() {
        assertThat(validate(Collections.nCopies(200, "aldi:1001"))).isEmpty();
        assertViolation(validate(Collections.nCopies(201, "aldi:1001")), "darf höchstens 200 Schlüssel enthalten");
    }

    @Test
    void emptyListIsValid() {
        assertThat(validate(List.of())).isEmpty();
    }

    @Test
    void keysAreRequired() {
        assertViolation(validate(null), "darf nicht leer sein");
    }

    @Test
    void nullAndMalformedElementsAreNoViolation() {
        assertThat(validate(Arrays.asList(null, "foo", ""))).isEmpty();
    }
}
