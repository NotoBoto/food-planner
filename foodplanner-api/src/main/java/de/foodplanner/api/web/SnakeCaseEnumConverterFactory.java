package de.foodplanner.api.web;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * Wandelt Request-Parameter in Enums um, akzeptiert aber nur die kleingeschriebene snake_case-Form
 * ({@code price_asc}, nicht {@code PRICE_ASC}); alles andere ergibt {@link IllegalArgumentException}
 * und damit 400 (§5.1). Für einen leeren String setzt Spring bei Parametern mit Default den Default
 * ein, noch bevor der Konverter läuft. Der Konverter selbst gibt dafür {@code null} zurück; das wirkt nur bei
 * Parametern ohne Default und endet dort als fehlender Parameter in 400.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class SnakeCaseEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            if (source.isEmpty()) {
                return null;
            }
            if (!source.equals(source.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Nur kleingeschriebene Werte erlaubt: " + source);
            }
            return (T) Enum.valueOf(targetType, source.toUpperCase(Locale.ROOT));
        };
    }
}
