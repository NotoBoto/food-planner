package de.foodplanner.ingestion;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "scrape", subcommands = AldiScrapeCommand.class, description = "Ermittelt und lädt Produktseiten.")
public class AldiCommand implements Runnable {

    @Override
    public void run() {
    }
}
