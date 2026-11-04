package de.foodplanner.api.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.foodplanner.application.port.SearchSort;

/** Registriert den snake_case-Konverter nur für die Enums, deren Parameter §5.1 in snake_case festlegt. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, SearchSort.class, new SnakeCaseEnumConverter<>(SearchSort.class));
    }
}
