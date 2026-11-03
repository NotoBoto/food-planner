package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import de.foodplanner.infrastructure.InfrastructureTestApplication;
import de.foodplanner.infrastructure.PostgresTestConfiguration;

/**
 * Die Trigramm-Schwelle der Suche (§5.5) kommt aus {@code connection-init-sql} des Pools, nicht aus der Datenbank.
 */
@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import(PostgresTestConfiguration.class)
class TrigramThresholdTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    void pooledConnectionHasThreshold() {
        assertThat(jdbc.sql("SHOW pg_trgm.word_similarity_threshold").query(String.class).single()).isEqualTo("0.4");
    }

    @Test
    void thresholdComesFromPoolNotFromDatabaseDefault() {
        assertThat(jdbc.sql("SELECT current_setting('pg_trgm.word_similarity_threshold')").query(String.class).single())
                .isEqualTo("0.4");
        assertThat(jdbc.sql("SELECT reset_val FROM pg_settings WHERE name = 'pg_trgm.word_similarity_threshold'")
                .query(String.class).single()).isEqualTo("0.6");
    }
}
