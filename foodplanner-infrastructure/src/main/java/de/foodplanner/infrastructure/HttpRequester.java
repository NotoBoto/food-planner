package de.foodplanner.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;
<<<<<<< HEAD
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true")
=======
import org.springframework.stereotype.Component;

@Component
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
public class HttpRequester {

    private final HttpClient client;
    private final HttpProperties properties;
    private final RequestGate gate;
    private final Sleeper sleeper;
    private final Clock clock;
    private final Semaphore permits;

    public HttpRequester(HttpProperties properties, Clock clock, Sleeper sleeper,
            RequestGate gate, ScrapeProperties scrapeProperties) {
<<<<<<< HEAD
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
=======
        this.client = HttpClient.newBuilder().build();
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
        this.properties = properties;
        this.clock = clock;
        this.gate = gate;
        this.sleeper = sleeper;
        this.permits = new Semaphore(scrapeProperties.scrapeConcurrency());
    }

    public HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        return sendWithRetry(uri);
    }

    private HttpResponse<String> sendWithRetry(URI uri) throws IOException, InterruptedException {
        permits.acquire();
        try {
            for (int attempt = 0; ; attempt++) {
                gate.awaitPermission();
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("User-Agent", properties.userAgent())
<<<<<<< HEAD
                        .timeout(Duration.ofSeconds(30))
=======
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
                        .GET()
                        .build();
                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (!isRetryable(response.statusCode()) || attempt >= 3) {
                        return response;
                    }
                    sleeper.sleep(retryDelay(response, attempt));
                } catch (IOException exception) {
                    if (attempt >= 3) {
                        throw exception;
                    }
                    sleeper.sleep(Duration.ofSeconds(1L << attempt));
                }
            }
        } finally {
            permits.release();
        }
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status >= 500 && status <= 599;
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) {
            try {
                return nonNegative(Duration.ofSeconds(Long.parseLong(retryAfter.trim())));
            } catch (NumberFormatException ignored) {
                try {
                    return nonNegative(Duration.between(clock.instant(),
                            ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()));
                } catch (RuntimeException ignoredDate) {
                    // Fall back to the defined exponential delay for malformed headers.
                }
            }
        }
        return nonNegative(Duration.ofSeconds(1L << attempt));
    }

    private static Duration nonNegative(Duration duration) {
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}
