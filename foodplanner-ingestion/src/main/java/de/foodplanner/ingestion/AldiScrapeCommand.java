package de.foodplanner.ingestion;

import de.foodplanner.application.PriceScraper;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "aldi", description = "Liest den neuesten Snapshot oder lädt mit --fetch live.")
public class AldiScrapeCommand implements Runnable {

    @Option(names = "--fetch", description = "Lädt Sitemap und Produktseiten live.")
    private boolean fetch;

    @Option(names = "--full", description = "Lädt auch Seiten mit unverändertem lastmod neu.")
    private boolean full;

    private final PriceScraper scraper;

    public AldiScrapeCommand(PriceScraper scraper) {
        this.scraper = scraper;
    }

    @Override
    public void run() {
        PriceScraper.ScrapeReport report = fetch
                ? scraper.scrape(full)
                : scraper.scrapeFromLatestSnapshot();
        System.out.printf("Aldi: %d betrachtet, %d geladen, %d übersprungen, %d fehlgeschlagen%n",
                report.considered(), report.fetched(), report.skipped(), report.failed());
    }
}
