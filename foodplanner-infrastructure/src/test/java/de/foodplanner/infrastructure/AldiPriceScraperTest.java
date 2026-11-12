package de.foodplanner.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.foodplanner.application.ScrapedPage;
import de.foodplanner.application.ScrapedPageRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AldiPriceScraperTest {

    private WireMockServer server;
    private Map<URI, ScrapedPage> pages;
    private AldiPriceScraper scraper;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        configureFor(server.port());
        pages = new HashMap<>();
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        HttpProperties http = new HttpProperties("FoodPlannerTest/1.0 (mailto:test@foodplanner.invalid)");
        RequestGate gate = new RequestGate(clock, duration -> { }, java.time.Duration.ZERO);
        HttpRequester requester = new HttpRequester(http, clock, duration -> { }, gate,
                new ScrapeProperties(100, 4, 0, server.baseUrl(), "/sitemap_products.xml"));
        scraper = new AldiPriceScraper(requester, new InMemoryPages(), 
                new ScrapeProperties(100, 4, 0, server.baseUrl(), "/sitemap_products.xml"),
                clock, http);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void refusesForbiddenSitemapBeforeLoadingProducts() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse()
                .withBody("User-agent: *\nDisallow: /sitemap_products.xml\n")));

        assertThatThrownBy(() -> scraper.scrape(false))
                .hasMessageContaining("robots.txt verbietet");
        server.verify(0, getRequestedFor(urlEqualTo("/product/1")));
    }

    @Test
    void skipsUnchangedLastmodButFetchesEntriesWithoutLastmodAgain() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse()
                .withBody("User-agent: *\nAllow: /\n")));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc><lastmod>2026-01-01T00:00:00Z</lastmod></url>
                          <url><loc>%s/product/2</loc></url>
                        </urlset>
                        """.formatted(server.baseUrl(), server.baseUrl()))));
        stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withStatus(200)));
        stubFor(get(urlEqualTo("/product/2")).willReturn(aResponse().withStatus(200)));

        ScrapedPage existing = new ScrapedPage(
                URI.create(server.baseUrl() + "/product/1"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
        pages.put(existing.url(), existing);

        assertThat(scraper.scrape(false)).isEqualTo(new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 1));
        scraper.scrape(false);
        server.verify(2, getRequestedFor(urlEqualTo("/product/2")));
    }

    private final class InMemoryPages implements ScrapedPageRepository {
        @Override
        public Optional<ScrapedPage> findByUrl(URI url) {
            return Optional.ofNullable(pages.get(url));
        }

        @Override
        public void save(ScrapedPage page) {
            pages.put(page.url(), page);
        }
    }
}
