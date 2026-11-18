package de.foodplanner.application;

public interface PriceScraper {

    ScrapeReport scrape(boolean full);

<<<<<<< HEAD
    record ScrapeReport(int considered, int fetched, int skipped, int failed) {
=======
    record ScrapeReport(int considered, int fetched, int skipped) {
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
    }
}
