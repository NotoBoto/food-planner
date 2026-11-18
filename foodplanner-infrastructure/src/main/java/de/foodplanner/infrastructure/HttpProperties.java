package de.foodplanner.infrastructure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
<<<<<<< HEAD
import java.util.regex.Matcher;
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73

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
<<<<<<< HEAD
        Matcher matcher = java.util.regex.Pattern.compile("[\\w.%+\\-]+@([\\w.-]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(userAgent);
        while (matcher.find()) {
            String domain = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            if (domain.equals("localhost")
                    || domain.endsWith(".localhost")
                    || domain.endsWith(".invalid")
                    || domain.endsWith(".test")
                    || domain.endsWith(".example")
                    || domain.startsWith("example.")
                    || domain.contains(".example.")) {
                return false;
            }
        }
        return true;
=======
        String value = userAgent.toLowerCase(java.util.Locale.ROOT);
        return !value.contains("@example.org")
                && !value.contains("@example.com")
                && !value.contains("@example.net")
                && !value.contains("@localhost");
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
    }
}
