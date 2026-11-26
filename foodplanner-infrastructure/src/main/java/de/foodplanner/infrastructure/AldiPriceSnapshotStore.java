package de.foodplanner.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.foodplanner.application.PriceScraper.ScrapeReport;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "ingestion.enabled", havingValue = "true")
public class AldiPriceSnapshotStore {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSSSSSSS'Z'").withZone(ZoneOffset.UTC);

    private final Path directory;
    private final Clock clock;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Autowired
    public AldiPriceSnapshotStore(Clock clock,
            @Value("${ingestion.snapshot-directory:data/snapshots}") String directory) {
        this(Path.of(directory), clock);
    }

    public AldiPriceSnapshotStore(Path directory, Clock clock) {
        this.directory = directory;
        this.clock = clock;
    }

    public Path write(List<SnapshotPage> pages, List<String> sitemapUrls, ScrapeReport report) {
        Instant generatedAt = clock.instant();
        Snapshot snapshot = new Snapshot(generatedAt.toString(), pages, sitemapUrls,
                new Coverage(report.considered(), report.fetched(), report.skipped(), report.failed()));
        Path file = directory.resolve("aldi-prices-" + FILE_TIMESTAMP.format(generatedAt) + ".json");
        try {
            Files.createDirectories(directory);
            Files.writeString(file, mapper.writeValueAsString(snapshot) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            return file;
        } catch (IOException exception) {
            throw new UncheckedIOException("Aldi-Snapshot konnte nicht geschrieben werden: " + file, exception);
        }
    }

    public Snapshot readLatest() {
        if (!Files.isDirectory(directory)) {
            throw missingSnapshot();
        }
        try (Stream<Path> files = Files.list(directory)) {
            Path latest = files
                    .filter(path -> path.getFileName().toString()
                            .matches("aldi-prices-[0-9]{8}T[0-9]{6}\\.[0-9]{9}Z\\.json"))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElseThrow(this::missingSnapshot);
            return mapper.readValue(Files.readString(latest, StandardCharsets.UTF_8), Snapshot.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Aldi-Snapshot konnte nicht gelesen werden: " + directory, exception);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Aldi-Snapshot ist ungültig: " + directory, exception);
        }
    }

    private IllegalStateException missingSnapshot() {
        return new IllegalStateException("Kein Aldi-Snapshot unter " + directory + " gefunden. "
                + "Bitte einmal mit --fetch ausführen.");
    }

    public record Snapshot(String generatedAt, List<SnapshotPage> pages, List<String> sitemapUrls,
            Coverage coverage) {
        public Snapshot {
            pages = List.copyOf(pages);
            sitemapUrls = List.copyOf(sitemapUrls);
        }
    }

    public record SnapshotPage(String url, String lastModified, String fetchedAt, String html) {
    }

    public record Coverage(int considered, int fetched, int skipped, int failed) {
    }
}
