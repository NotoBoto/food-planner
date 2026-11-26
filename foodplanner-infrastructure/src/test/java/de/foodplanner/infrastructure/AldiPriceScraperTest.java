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
import de.foodplanner.domain.RawProduct;
import de.foodplanner.infrastructure.aldi.AldiProductParser;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AldiPriceScraperTest {

    @TempDir
    Path snapshotDir;

    private WireMockServer server;
    private Map<URI, ScrapedPage> pages;
    private AldiPriceScraper scraper;
    private AldiPriceSnapshotStore snapshots;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        configureFor(server.port());
        pages = new HashMap<>();
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        HttpProperties http = new HttpProperties("FoodPlannerTest/1.0 (mailto:test@foodplanner.dev)");
        RequestGate gate = new RequestGate(clock, duration -> { }, Duration.ZERO);
        HttpRequester requester = new HttpRequester(http, clock, duration -> { }, gate,
                new ScrapeProperties(2, 4, 0, server.baseUrl(), "/sitemap_products.xml"));
        snapshots = new AldiPriceSnapshotStore(snapshotDir, clock);
        scraper = new AldiPriceScraper(requester, new InMemoryPages(),
                new ScrapeProperties(2, 4, 0, server.baseUrl(), "/sitemap_products.xml"),
                clock, http, snapshots);
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
    void refusesForbiddenProductBeforeLoadingIt() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse()
                .withBody("User-agent: *\nDisallow: /product/\n")));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc></url>
                        </urlset>
                        """.formatted(server.baseUrl()))));

        assertThatThrownBy(() -> scraper.scrape(false))
                .hasMessageContaining("robots.txt verbietet");
        server.verify(0, getRequestedFor(urlEqualTo("/product/1")));
    }

    @Test
    void missingRobotsTxtAllowsAllPaths() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc></url>
                        </urlset>
                        """.formatted(server.baseUrl()))));
        stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withStatus(200)));

        assertThat(scraper.scrape(false).fetched()).isEqualTo(1);
        server.verify(1, getRequestedFor(urlEqualTo("/product/1")));
    }

    @Test
    void liveFetchWritesPagesFullSitemapAndCoverageToSnapshot() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc></url>
                          <url><loc>%s/product/2</loc></url>
                          <url><loc>%s/product/3</loc></url>
                        </urlset>
                        """.formatted(server.baseUrl(), server.baseUrl(), server.baseUrl()))));
        stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withBody("<html>one</html>")));
        stubFor(get(urlEqualTo("/product/2")).willReturn(aResponse().withStatus(404)));

        assertThat(scraper.scrape(true)).isEqualTo(
                new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 0, 1));

        AldiPriceSnapshotStore.Snapshot snapshot = snapshots.readLatest();
        assertThat(snapshot.sitemapUrls()).containsExactly(
                server.baseUrl() + "/product/1",
                server.baseUrl() + "/product/2",
                server.baseUrl() + "/product/3");
        assertThat(snapshot.pages()).singleElement().satisfies(page -> {
            assertThat(page.url()).isEqualTo(server.baseUrl() + "/product/1");
            assertThat(page.html()).isEqualTo("<html>one</html>");
        });
        assertThat(snapshot.coverage()).isEqualTo(new AldiPriceSnapshotStore.Coverage(2, 1, 0, 1));
        assertThat(snapshot.generatedAt()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(snapshotDir.resolve("aldi-prices-20260101T000000.000000000Z.json")).exists();
        assertThat(readSnapshotJson())
                .contains("\"generatedAt\"", "\"sitemapUrls\"", "\"coverage\"", "\"considered\"")
                .doesNotContain("\"generated_at\"", "\"sitemap_urls\"");
    }

    @Test
    void snapshotReplayDoesNotMakeNetworkRequests() {
        URI productUrl = URI.create(server.baseUrl() + "/product/1");
        snapshots.write(
                List.of(new AldiPriceSnapshotStore.SnapshotPage(productUrl.toString(), null,
                        "2026-01-01T00:00:00Z", "<html>one</html>")),
                List.of(productUrl.toString(), server.baseUrl() + "/product/2"),
                new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 0, 1));

        assertThat(scraper.scrapeFromLatestSnapshot()).isEqualTo(
                new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 0, 1));

        server.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
        assertThat(pages).containsKey(productUrl);
    }

    @Test
    void snapshotRoundTripKeepsTheSameParsedProduct() {
        URI productUrl = URI.create(server.baseUrl() + "/product/1");
        String html = """
                <html><script type="application/ld+json">
                {"@type":"Product","name":"Milch","offers":{"price":"1.29"}}
                </script></html>
                """;
        AldiProductParser parser = new AldiProductParser();
        RawProduct liveProduct = parser.parse(html, productUrl.toString()).orElseThrow();
        snapshots.write(List.of(new AldiPriceSnapshotStore.SnapshotPage(
                        productUrl.toString(), null, "2026-01-01T00:00:00Z", html)),
                List.of(productUrl.toString()),
                new de.foodplanner.application.PriceScraper.ScrapeReport(1, 1, 0, 0));

        AldiPriceSnapshotStore.Snapshot snapshot = snapshots.readLatest();
        RawProduct snapshotProduct = parser.parse(
                snapshot.pages().getFirst().html(), snapshot.pages().getFirst().url()).orElseThrow();

        assertThat(snapshotProduct).isEqualTo(liveProduct);
    }

    @Test
    void missingSnapshotFailsWithAnActionableMessage() {
        assertThatThrownBy(scraper::scrapeFromLatestSnapshot)
                .hasMessageContaining("Kein Aldi-Snapshot")
                .hasMessageContaining("--fetch");
        server.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
    }

    @Test
    void newestSnapshotIsSelectedByChronologicalFilename() {
        AldiPriceSnapshotStore older = new AldiPriceSnapshotStore(snapshotDir,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        AldiPriceSnapshotStore newer = new AldiPriceSnapshotStore(snapshotDir,
                Clock.fixed(Instant.parse("2026-02-01T00:00:00Z"), ZoneOffset.UTC));
        var emptyReport = new de.foodplanner.application.PriceScraper.ScrapeReport(0, 0, 0, 0);
        older.write(List.of(), List.of(), emptyReport);
        newer.write(List.of(), List.of(), emptyReport);

        assertThat(older.readLatest().generatedAt()).isEqualTo("2026-02-01T00:00:00Z");
    }

    private String readSnapshotJson() {
        try {
            return Files.readString(snapshotDir.resolve("aldi-prices-20260101T000000.000000000Z.json"));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @Test
    void repeatedTooManyRequestsForRobotsTxtAbortsBeforeSitemap() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> scraper.scrape(false))
                .hasMessageContaining("HTTP 429");
        server.verify(0, getRequestedFor(urlEqualTo("/sitemap_products.xml")));
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

        assertThat(scraper.scrape(false)).isEqualTo(new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 1, 0));
        scraper.scrape(false);
        server.verify(2, getRequestedFor(urlEqualTo("/product/2")));
    }

    @Test
    void fullFetchLoadsUnchangedPagesAgain() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse()
                .withBody("User-agent: *\nAllow: /\n")));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc><lastmod>2026-01-01T00:00:00Z</lastmod></url>
                        </urlset>
                        """.formatted(server.baseUrl()))));
        stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withStatus(200)));
        pages.put(URI.create(server.baseUrl() + "/product/1"), new ScrapedPage(
                URI.create(server.baseUrl() + "/product/1"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")));

        assertThat(scraper.scrape(true).fetched()).isEqualTo(1);
        server.verify(1, getRequestedFor(urlEqualTo("/product/1")));
    }

    @Test
    void skipsMissingProductsWithoutAbortingTheRun() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse()
                .withBody("User-agent: *\nAllow: /\n")));
        stubFor(get(urlEqualTo("/sitemap_products.xml")).willReturn(aResponse()
                .withBody("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                          <url><loc>%s/product/1</loc></url>
                          <url><loc>%s/product/2</loc></url>
                        </urlset>
                        """.formatted(server.baseUrl(), server.baseUrl()))));
        stubFor(get(urlEqualTo("/product/1")).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlEqualTo("/product/2")).willReturn(aResponse().withStatus(200)));

        assertThat(scraper.scrape(false)).isEqualTo(
                new de.foodplanner.application.PriceScraper.ScrapeReport(2, 1, 0, 1));
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
