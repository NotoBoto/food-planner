package de.foodplanner.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "http")
public record HttpProperties(
        @NotBlank
        @Pattern(regexp = ".*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*",
                message = "http.user-agent muss eine echte Kontaktadresse enthalten")
        String userAgent) {
}
