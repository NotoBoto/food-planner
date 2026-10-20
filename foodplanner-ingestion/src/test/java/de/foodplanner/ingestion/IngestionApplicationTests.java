package de.foodplanner.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import picocli.CommandLine;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class IngestionApplicationTests {

    @Autowired
    IngestionCommand command;

    @Autowired
    SpringPicocliFactory factory;

    @Autowired
    IngestionRunner runner;

    @Test
    void runWithoutArgumentsPrintsUsage(CapturedOutput output) {
        runner.run();
        assertThat(runner.getExitCode()).isZero();
        assertThat(output.getOut()).contains("Usage: foodplanner-ingestion");
    }

    @Test
    void helpOptionPrintsUsageAndExitsZero() {
        StringWriter out = new StringWriter();
        int exitCode = new CommandLine(command, factory)
                .setOut(new PrintWriter(out))
                .execute("--help");
        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .contains("Usage: foodplanner-ingestion")
                .contains("--help")
                .contains("--version");
    }
}
