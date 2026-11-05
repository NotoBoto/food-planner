package de.foodplanner.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

@Component
public class HttpRequester implements AutoCloseable {

    private final HttpClient client;
    private final HttpProperties properties;
    private final RequestGate gate;
    private final Sleeper sleeper;
    private final Clock clock;
    private final Semaphore permits;
    private final ExecutorService executor;

    public HttpRequester(HttpProperties properties, Clock clock, Sleeper sleeper,
            RequestGate gate, ScrapeProperties scrapeProperties) {
        this.client = HttpClient.newBuilder().build();
        this.properties = properties;
        this.clock = clock;
        this.gate = gate;
        this.sleeper = sleeper;
        this.permits = new Semaphore(scrapeProperties.scrapeConcurrency());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        try {
            return executor.submit(() -> sendWithRetry(uri)).get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof InterruptedException interruptedException) {
                throw interruptedException;
            }
            throw new IOException("HTTP-Anfrage fehlgeschlagen: " + uri, cause);
        }
    }

    private HttpResponse<String> sendWithRetry(URI uri) throws IOException, InterruptedException {
        permits.acquire();
        try {
            for (int attempt = 0; ; attempt++) {
                gate.awaitPermission();
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("User-Agent", properties.userAgent())
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
                return Duration.ofSeconds(Long.parseLong(retryAfter.trim()));
            } catch (NumberFormatException ignored) {
                try {
                    return Duration.between(clock.instant(),
                            ZonedDateTime.parse(retryAfter).toInstant());
                } catch (RuntimeException ignoredDate) {
                    // Fall back to the defined exponential delay for malformed headers.
                }
            }
        }
        return Duration.ofSeconds(1L << attempt);
    }

    @Override
    public void close() {
        executor.close();
    }
}
