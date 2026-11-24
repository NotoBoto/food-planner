package de.foodplanner.application.products.batch;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Klassenweite Prüfung: Jeder Schlüssel in {@code keys} ist höchstens {@value GetProductsBatchQuery#MAX_KEY_LENGTH}
 * Zeichen lang. Gemeldet wird am Feld {@code keys}, nicht am Listenelement, damit die Antwort {@code errors.keys}
 * enthält.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = KeyLengthLimitedValidator.class)
public @interface KeyLengthLimited {

    String message() default "jeder Schlüssel darf höchstens 64 Zeichen lang sein";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
