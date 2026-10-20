package de.foodplanner.ingestion;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Component
@Command(
        name = "foodplanner-ingestion",
        mixinStandardHelpOptions = true,
        version = "foodplanner-ingestion 0.1.0-SNAPSHOT",
        description = "FoodPlanner-Ingestion: scrape, search-off, review, seed export.")
public class IngestionCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
