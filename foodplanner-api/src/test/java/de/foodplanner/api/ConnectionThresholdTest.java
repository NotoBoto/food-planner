package de.foodplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

/** Die API bekommt die Schwelle aus der Auto-Konfiguration von {@code infrastructure}, ohne eigene Einstellung (§5.5). */
@SpringBootTest
@Import(PostgresTestConfiguration.class)
class ConnectionThresholdTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void poolConnectionsUseWordSimilarityThreshold04() {
        String value = jdbc.sql("SHOW pg_trgm.word_similarity_threshold").query(String.class).single();

        assertThat(value).isEqualTo("0.4");
    }
}
