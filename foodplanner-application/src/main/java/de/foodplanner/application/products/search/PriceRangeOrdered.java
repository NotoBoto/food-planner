package de.foodplanner.application.products.search;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Klassenweite Prüfung: {@code minPriceCents} darf nicht größer als {@code maxPriceCents} sein (§5.1). */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = PriceRangeOrderedValidator.class)
public @interface PriceRangeOrdered {

    String message() default "darf nicht größer als maxPriceCents sein";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
