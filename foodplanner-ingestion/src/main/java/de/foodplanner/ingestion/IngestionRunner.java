package de.foodplanner.ingestion;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import picocli.CommandLine;

@Component
public class IngestionRunner implements CommandLineRunner, ExitCodeGenerator {

    private final IngestionCommand command;
    private final SpringPicocliFactory factory;
    private int exitCode;

    IngestionRunner(IngestionCommand command, SpringPicocliFactory factory) {
        this.command = command;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(command, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
