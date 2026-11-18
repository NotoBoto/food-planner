package de.foodplanner.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import de.foodplanner.application.PriceScraper;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AldiScrapeCommandTest {

    @Test
    void fetchInvokesScraperAndFullIsForwarded() {
        PriceScraper scraper = mock(PriceScraper.class);
<<<<<<< HEAD
        when(scraper.scrape(true)).thenReturn(new PriceScraper.ScrapeReport(1, 1, 0, 0));
=======
        when(scraper.scrape(true)).thenReturn(new PriceScraper.ScrapeReport(1, 1, 0));
>>>>>>> 6fec8dab926e0202e2ddbdf21a29b9a0bba6fa73
        AldiScrapeCommand command = new AldiScrapeCommand(scraper);

        int exitCode = new CommandLine(command).execute("--fetch", "--full");

        assertThat(exitCode).isZero();
        verify(scraper).scrape(true);
    }

    @Test
    void withoutFetchDoesNotLoadPages() {
        PriceScraper scraper = mock(PriceScraper.class);
        AldiScrapeCommand command = new AldiScrapeCommand(scraper);

        assertThat(new CommandLine(command).execute()).isZero();

        verify(scraper, org.mockito.Mockito.never()).scrape(org.mockito.ArgumentMatchers.anyBoolean());
    }
}
