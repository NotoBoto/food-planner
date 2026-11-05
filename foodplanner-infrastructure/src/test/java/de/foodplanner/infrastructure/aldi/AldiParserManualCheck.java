package de.foodplanner.infrastructure.aldi;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.foodplanner.domain.ScrapedRowValidator;

class AldiParserManualCheck {

    @Test
    void printParsedPages() throws Exception {
        String dir = System.getProperty("pages");
        assumeTrue(dir != null, "-Dpages=<Ordner mit gespeicherten HTML-Dateien> setzen");

        var parser = new AldiProductParser();
        var validator = new ScrapedRowValidator(Set.of());
        try (var files = Files.list(Path.of(dir))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".html")).sorted().toList()) {
                var raw = parser.parse(Files.readString(file), "https://www.aldi-sued.de/");
                System.out.println("\n=== " + file.getFileName());
                System.out.println("roh:       " + raw.map(Object::toString).orElse("KEIN PRODUKT GEFUNDEN"));
                System.out.println("validiert: " + raw.flatMap(validator::validate)
                        .map(Object::toString).orElse("VERWORFEN"));
            }
        }
    }
}