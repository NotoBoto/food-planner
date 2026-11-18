package de.foodplanner.application.shoppinglist.summarise;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import de.foodplanner.application.shoppinglist.summarise.SummariseShoppingListQuery.Item;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Meldet {@code null}-Einträge und doppelte Schlüssel am Feld {@code items}. Eine fehlende Liste und Zeilen ohne
 * {@code productKey} sind hier gültig; die meldet {@code @NotNull}.
 */
public class ItemsWellFormedValidator implements ConstraintValidator<ItemsWellFormed, SummariseShoppingListQuery> {

    private String duplicateMessage = "";

    @Override
    public void initialize(ItemsWellFormed annotation) {
        duplicateMessage = annotation.duplicateMessage();
    }

    @Override
    public boolean isValid(SummariseShoppingListQuery query, ConstraintValidatorContext context) {
        List<@Nullable Item> items = query.items();
        if (items == null) {
            return true;
        }
        if (items.stream().anyMatch(Objects::isNull)) {
            return reject(context, context.getDefaultConstraintMessageTemplate());
        }
        Set<String> seen = new HashSet<>();
        for (Item item : items) {
            if (item != null && item.productKey() != null && !seen.add(item.productKey())) {
                return reject(context, duplicateMessage);
            }
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode("items").addConstraintViolation();
        return false;
    }
}
