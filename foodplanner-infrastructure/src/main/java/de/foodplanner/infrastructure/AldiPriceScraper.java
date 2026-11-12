package de.foodplanner.infrastructure;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import de.foodplanner.application.PriceScraper;
import de.foodplanner.application.ScrapedPage;
import de.foodplanner.application.ScrapedPageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.stereotype.Component;

@Component
public class AldiPriceScraper implements PriceScraper {

    private final HttpRequester requester;
    private final ScrapedPageRepository pages;
    private final ScrapeProperties properties;
    private final Clock clock;
    private final HttpProperties httpProperties;

    public AldiPriceScraper(HttpRequester requester, ScrapedPageRepository pages,
            ScrapeProperties properties, Clock clock, HttpProperties httpProperties) {
        this.requester = requester;
        this.pages = pages;
        this.properties = properties;
        this.clock = clock;
        this.httpProperties = httpProperties;
    }

    @Override
    public ScrapeReport scrape(boolean full) {
        URI sitemapUri = properties.sitemapUri();
        URI robotsUri = properties.baseUri().resolve("/robots.txt");

        try {
            BaseRobotRules rules = readRules(robotsUri);
            ensureAllowed(rules, sitemapUri);
            HttpResponse<String> sitemapResponse = requester.get(sitemapUri);
            requireSuccess(sitemapUri, sitemapResponse);
            List<SiteMapURL> entries = readEntries(sitemapUri, sitemapResponse.body()).stream()
                    .limit(properties.aldiMaxProducts())
                    .toList();
            int skipped = 0;
            List<FetchEntry> toFetch = new java.util.ArrayList<>();
            for (SiteMapURL entry : entries) {
                URI productUri = URI.create(entry.getUrl().toString());
                ensureAllowed(rules, productUri);
                Instant lastModified = entry.getLastModified() == null
                        ? null : entry.getLastModified().toInstant();
                if (!full && pages.findByUrl(productUri)
                        .map(previous -> lastModified != null
                                && Objects.equals(previous.lastModified(), lastModified))
                        .orElse(false)) {
                    skipped++;
                    continue;
                }
                toFetch.add(new FetchEntry(productUri, lastModified));
            }
            int fetched = 0;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<Void>> futures = toFetch.stream()
                        .map(entry -> executor.submit((java.util.concurrent.Callable<Void>) () -> {
                            fetchAndSave(entry);
                            return null;
                        }))
                        .toList();
                for (Future<Void> future : futures) {
                    future.get();
                    fetched++;
                }
            }
            return new ScrapeReport(entries.size(), fetched, skipped);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException ioException) {
                throw new UncheckedIOException("Aldi-Abruf fehlgeschlagen: " + ioException.getMessage(), ioException);
            }
            if (exception.getCause() instanceof InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Aldi-Abruf wurde unterbrochen", interruptedException);
            }
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Aldi-Abruf fehlgeschlagen", exception.getCause());
        } catch (IOException exception) {
            throw new UncheckedIOException("Aldi-Abruf fehlgeschlagen: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Aldi-Abruf wurde unterbrochen", exception);
        }
    }

    private void fetchAndSave(FetchEntry entry) throws IOException, InterruptedException {
        HttpResponse<String> productResponse = requester.get(entry.url());
        requireSuccess(entry.url(), productResponse);
        pages.save(new ScrapedPage(entry.url(), entry.lastModified(), clock.instant()));
    }

    private record FetchEntry(URI url, Instant lastModified) {
    }

    private BaseRobotRules readRules(URI robotsUri) throws IOException, InterruptedException {
        HttpResponse<String> response = requester.get(robotsUri);
        requireSuccess(robotsUri, response);
        return new SimpleRobotRulesParser().parseContent(
                robotsUri.toString(), response.body().getBytes(StandardCharsets.UTF_8),
                "text/plain", httpProperties.userAgent());
    }

    private static void ensureAllowed(BaseRobotRules rules, URI target) {
        if (!rules.isAllowed(target.toString())) {
            throw new IllegalStateException("robots.txt verbietet den Abruf von " + target);
        }
    }

    private static Collection<SiteMapURL> readEntries(URI sitemapUri, String body) throws IOException {
        AbstractSiteMap parsed;
        try {
            parsed = new SiteMapParser().parseSiteMap(
                    body.getBytes(StandardCharsets.UTF_8), sitemapUri.toURL());
        } catch (crawlercommons.sitemaps.UnknownFormatException exception) {
            throw new IllegalStateException("Ungültiges Sitemap-Format: " + sitemapUri, exception);
        }
        if (!(parsed instanceof SiteMap siteMap)) {
            throw new IllegalStateException("Die Sitemap ist ein Sitemap-Index; Produkt-Sitemaps werden nicht rekursiv geladen");
        }
        return siteMap.getSiteMapUrls();
    }

    private static void requireSuccess(URI uri, HttpResponse<?> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " beim Abruf von " + uri);
        }
    }
}
