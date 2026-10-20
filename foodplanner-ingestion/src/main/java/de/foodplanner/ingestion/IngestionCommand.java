package de.foodplanner.ingestion;

import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Component
@Command(
        name = "foodplanner-ingestion",
        mixinStandardHelpOptions = true,
        version = "foodplanner-ingestion 0.1.0-SNAPSHOT",
        description = "FoodPlanner-Ingestion: scrape, search-off, review, seed export.")
public class IngestionCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}
