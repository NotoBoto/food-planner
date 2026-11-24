package de.foodplanner.application.products.batch;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Meldet einen zu langen Schlüssel am Feld {@code keys}; fehlende Liste und {@code null}-Einträge sind gültig. */
public class KeyLengthLimitedValidator implements ConstraintValidator<KeyLengthLimited, GetProductsBatchQuery> {

    @Override
    public boolean isValid(GetProductsBatchQuery query, ConstraintValidatorContext context) {
        if (query.keys() == null || query.keys().stream()
                .allMatch(key -> key == null || key.length() <= GetProductsBatchQuery.MAX_KEY_LENGTH)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("keys")
                .addConstraintViolation();
        return false;
    }
}
