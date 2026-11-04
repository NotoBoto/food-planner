package de.foodplanner.api.web;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;

/**
 * Wandelt einen Request-Parameter in ein bestimmtes Enum um, akzeptiert aber nur die kleingeschriebene snake_case-Form
 * ({@code price_asc}, nicht {@code PRICE_ASC}); alles andere ergibt {@link IllegalArgumentException} und damit 400
 * (§5.1). Für einen leeren String setzt Spring bei Parametern mit Default den Default ein, noch bevor der Konverter
 * läuft. Der Konverter selbst gibt dafür {@code null} zurück; das wirkt nur bei Parametern ohne Default und endet dort
 * als fehlender Parameter in 400.
 *
 * <p>Gilt nur für die Enums, für die {@link WebConfig} ihn ausdrücklich registriert; alle anderen Enums behalten die
 * Standardumwandlung von Spring.
 *
 * @param <E> das Ziel-Enum
 */
final class SnakeCaseEnumConverter<E extends Enum<E>> implements Converter<String, E> {

    private final Class<E> targetType;

    SnakeCaseEnumConverter(Class<E> targetType) {
        this.targetType = targetType;
    }

    @Override
    public @Nullable E convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        if (!source.equals(source.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Nur kleingeschriebene Werte erlaubt: " + source);
        }
        return Enum.valueOf(targetType, source.toUpperCase(Locale.ROOT));
    }
}
