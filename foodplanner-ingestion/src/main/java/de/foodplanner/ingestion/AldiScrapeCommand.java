package de.foodplanner.ingestion;

import de.foodplanner.application.PriceScraper;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name = "aldi", description = "Lädt Aldi-Produktseiten unter Beachtung von robots.txt.")
public class AldiScrapeCommand implements Runnable {

    @Option(names = "--fetch", description = "Lädt Sitemap und Produktseiten.")
    private boolean fetch;

    @Option(names = "--full", description = "Lädt auch Seiten mit unverändertem lastmod neu.")
    private boolean full;

    private final PriceScraper scraper;

    public AldiScrapeCommand(PriceScraper scraper) {
        this.scraper = scraper;
    }

    @Override
    public void run() {
        if (!fetch) {
            return;
        }
        PriceScraper.ScrapeReport report = scraper.scrape(full);
        System.out.printf("Aldi: %d betrachtet, %d geladen, %d übersprungen%n",
                report.considered(), report.fetched(), report.skipped());
    }
}
