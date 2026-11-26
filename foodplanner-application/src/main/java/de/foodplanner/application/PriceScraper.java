package de.foodplanner.application;

public interface PriceScraper {

    ScrapeReport scrape(boolean full);

    ScrapeReport scrapeFromLatestSnapshot();

    record ScrapeReport(int considered, int fetched, int skipped, int failed) {
    }
}
