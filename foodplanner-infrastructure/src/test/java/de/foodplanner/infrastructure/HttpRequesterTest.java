package de.foodplanner.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpRequesterTest {

    private WireMockServer server;
    private List<Duration> waits;
    private HttpRequester requester;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        configureFor(server.port());
        waits = new ArrayList<>();
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T07:27:00Z"), ZoneOffset.UTC);
        RequestGate gate = new RequestGate(clock, waits::add, Duration.ZERO);
        requester = new HttpRequester(
                new HttpProperties("FoodPlannerTest/1.0 (mailto:test@foodplanner.dev)"),
                clock, waits::add, gate,
                new ScrapeProperties(100, 4, 0, "http://localhost", "/sitemap.xml"));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void retriesServerErrorsWithExponentialBackoffAndSendsUserAgent() throws Exception {
        stubFor(get(urlEqualTo("/retry"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));
        stubFor(get(urlEqualTo("/retry"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));
        stubFor(get(urlEqualTo("/retry"))
                .inScenario("retry")
                .whenScenarioStateIs("third")
                .willReturn(aResponse().withStatus(200).withBody("ok")));

        assertThat(requester.get(URI.create(server.baseUrl() + "/retry")).statusCode()).isEqualTo(200);
        assertThat(waits).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
        server.verify(3, getRequestedFor(urlEqualTo("/retry"))
                .withHeader("User-Agent", equalTo("FoodPlannerTest/1.0 (mailto:test@foodplanner.dev)")));
    }

    @Test
    void usesHttpDateRetryAfterAndNeverWaitsNegativeDurations() throws Exception {
        stubFor(get(urlEqualTo("/limited"))
                .willReturn(aResponse().withStatus(429)
                        .withHeader("Retry-After", "Wed, 21 Oct 2015 07:28:00 GMT")));

        assertThat(requester.get(URI.create(server.baseUrl() + "/limited")).statusCode()).isEqualTo(429);
        assertThat(waits).containsExactly(Duration.ZERO, Duration.ZERO, Duration.ZERO);
    }

    @Test
    void doesNotRetryNotFound() throws Exception {
        stubFor(get(urlEqualTo("/missing")).willReturn(aResponse().withStatus(404)));

        assertThat(requester.get(URI.create(server.baseUrl() + "/missing")).statusCode()).isEqualTo(404);
        server.verify(1, getRequestedFor(urlEqualTo("/missing")));
    }

    @Test
    void retriesConnectionResetAndStopsAfterThreeRetries() {
        stubFor(get(urlEqualTo("/reset"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> requester.get(URI.create(server.baseUrl() + "/reset")))
                .isInstanceOf(java.io.IOException.class);
        assertThat(waits).containsExactly(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4));
        server.verify(4, getRequestedFor(urlEqualTo("/reset")));
    }

    @Test
    void exhaustsServerErrorRetriesAfterFourCalls() throws Exception {
        stubFor(get(urlEqualTo("/always-failing"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(requester.get(URI.create(server.baseUrl() + "/always-failing")).statusCode())
                .isEqualTo(503);
        assertThat(waits).containsExactly(
                Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4));
        server.verify(4, getRequestedFor(urlEqualTo("/always-failing")));
    }

    @Test
    void retriesRetryAfterSecondsBeforeFallingBack() throws Exception {
        stubFor(get(urlEqualTo("/limited-seconds"))
                .inScenario("retry-after")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429)
                        .withHeader("Retry-After", "3"))
                .willSetStateTo("done"));
        stubFor(get(urlEqualTo("/limited-seconds"))
                .inScenario("retry-after")
                .whenScenarioStateIs("done")
                .willReturn(aResponse().withStatus(200)));

        assertThat(requester.get(URI.create(server.baseUrl() + "/limited-seconds")).statusCode())
                .isEqualTo(200);
        assertThat(waits).containsExactly(Duration.ofSeconds(3));
    }

    @Test
    void limitsConcurrentRequestsToConfiguredConcurrency() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        var httpServer = com.sun.net.httpserver.HttpServer
                .create(new java.net.InetSocketAddress("localhost", 0), 0);
        var serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        httpServer.setExecutor(serverExecutor);
        httpServer.createContext("/concurrent", exchange -> {
            int current = active.incrementAndGet();
            maximum.accumulateAndGet(current, Math::max);
            try {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException(exception);
                }
                exchange.sendResponseHeaders(200, 0);
            } finally {
                exchange.getResponseBody().close();
                active.decrementAndGet();
            }
        });
        httpServer.start();
        try {
            URI uri = URI.create("http://localhost:" + httpServer.getAddress().getPort() + "/concurrent");
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = IntStream.range(0, 8)
                        .mapToObj(index -> executor.submit(() -> requester.get(uri)))
                        .toList();
                for (var future : futures) {
                    future.get();
                }
            }
            assertThat(maximum).hasValue(4);
        } finally {
            httpServer.stop(0);
            serverExecutor.close();
        }
    }
}
