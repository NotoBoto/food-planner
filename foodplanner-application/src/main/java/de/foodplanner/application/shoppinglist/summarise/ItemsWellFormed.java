package de.foodplanner.application.shoppinglist.summarise;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Klassenweite Prüfung der Zeilen: kein {@code null}-Eintrag (sonst überspränge {@code @Valid} ihn) und jeder
 * {@code productKey} höchstens einmal, denn pro Schlüssel gibt es höchstens eine Zeile (§7.2). Gemeldet wird am Feld
 * {@code items}, damit die Antwort {@code errors.items} enthält; eine Constraint am Listenelement landete dagegen unter
 * dem Knoten {@code <list element>}.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ItemsWellFormedValidator.class)
public @interface ItemsWellFormed {

    String message() default "darf keine leeren Einträge enthalten";

    String duplicateMessage() default "jeder productKey darf nur einmal vorkommen";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
