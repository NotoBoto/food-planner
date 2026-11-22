package de.foodplanner.api.web;

import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.foodplanner.application.port.SearchSort;

/** Registriert den snake_case-Konverter nur für die Enums, deren Parameter §5.1 in snake_case festlegt. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    /**
     * Enums, die als Request-Parameter in snake_case angenommen werden ({@code price_asc}). Auch
     * {@link OpenApiConfig} liest diese Liste, damit API und Spezifikation nie auseinanderlaufen.
     */
    static final Set<Class<? extends Enum<?>>> SNAKE_CASE_ENUMS = Set.of(SearchSort.class);

    @Override
    public void addFormatters(FormatterRegistry registry) {
        for (Class<? extends Enum<?>> type : SNAKE_CASE_ENUMS) {
            register(registry, type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(FormatterRegistry registry, Class<? extends Enum<?>> type) {
        // Der Konverter braucht den genauen Enum-Typ, die Liste kennt nur Class<? extends Enum<?>>.
        registry.addConverter(String.class, (Class) type, new SnakeCaseEnumConverter(type));
    }
}
