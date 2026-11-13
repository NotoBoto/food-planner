package de.foodplanner.application.products.search;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Meldet „min > max" am Feld {@code minPriceCents}; offene Bereiche sind gültig. */
public class PriceRangeOrderedValidator implements ConstraintValidator<PriceRangeOrdered, SearchProductsQuery> {

    @Override
    public boolean isValid(SearchProductsQuery query, ConstraintValidatorContext context) {
        Long min = query.minPriceCents();
        Long max = query.maxPriceCents();
        if (min == null || max == null || min <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("minPriceCents")
                .addConstraintViolation();
        return false;
    }
}
