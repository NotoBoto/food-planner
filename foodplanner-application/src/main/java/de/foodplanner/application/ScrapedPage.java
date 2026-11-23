package de.foodplanner.application;

import java.net.URI;
import java.time.Instant;

public record ScrapedPage(URI url, Instant lastModified, Instant fetchedAt) {
}
