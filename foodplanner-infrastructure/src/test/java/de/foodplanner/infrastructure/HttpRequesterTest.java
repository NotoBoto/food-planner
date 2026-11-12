package de.foodplanner.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
                new HttpProperties("FoodPlannerTest/1.0 (mailto:test@foodplanner.invalid)"),
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
                .withHeader("User-Agent", equalTo("FoodPlannerTest/1.0 (mailto:test@foodplanner.invalid)")));
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
}
