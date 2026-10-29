package de.foodplanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PostgresImageTest {

    @Test
    void testcontainersUsesTheImageOfTheComposeService() throws IOException {
        Path compose = Path.of("..", "docker-compose.yml");

        assertThat(Files.readAllLines(compose))
                .map(String::strip)
                .filteredOn(line -> line.startsWith("image:"))
                .containsExactly("image: " + PostgresTestConfiguration.IMAGE);
    }
}
