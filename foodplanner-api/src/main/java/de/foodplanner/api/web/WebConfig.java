package de.foodplanner.api.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registriert den snake_case-Enum-Konverter für alle Request-Parameter. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new SnakeCaseEnumConverterFactory());
    }
}
