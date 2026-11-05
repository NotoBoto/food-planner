package de.foodplanner.application;

public interface PriceScraper {

    ScrapeReport scrape(boolean full);

    record ScrapeReport(int considered, int fetched, int skipped) {
    }
}
