package de.foodplanner.infrastructure;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ingestion")
public record ScrapeProperties(
        @Min(1) @Max(10_000) int aldiMaxProducts,
        @Min(1) int scrapeConcurrency,
        @DecimalMin("0.0") double scrapeDelaySeconds,
        @NotBlank String aldiBaseUrl,
        @NotBlank String aldiSitemapPath) {

    public URI baseUri() {
        return URI.create(aldiBaseUrl);
    }

    public URI sitemapUri() {
        return baseUri().resolve(aldiSitemapPath);
    }
}
