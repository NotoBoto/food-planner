package de.foodplanner.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "http")
public record HttpProperties(
        @NotBlank
        @Pattern(regexp = ".*[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}.*",
                message = "http.user-agent muss eine echte Kontaktadresse enthalten")
        String userAgent) {

    @AssertTrue(message = "http.user-agent darf keine Platzhalteradresse enthalten")
    public boolean isContactAddressValid() {
        if (userAgent == null) {
            return false;
        }
        String value = userAgent.toLowerCase(java.util.Locale.ROOT);
        return !value.contains("@example.org")
                && !value.contains("@example.com")
                && !value.contains("@example.net")
                && !value.contains("@localhost");
    }
}
