package de.foodplanner.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.zaxxer.hikari.HikariDataSource;

/**
 * {@link TrigramThresholdAutoConfiguration} ohne Datenbank: Der Pool baut erst beim ersten {@code getConnection()} eine
 * Verbindung auf, deshalb genügt ein unkonfigurierter {@link HikariDataSource}.
 */
@ExtendWith(OutputCaptureExtension.class)
class TrigramThresholdAutoConfigurationTest {

    @Test
    void setsThresholdWhenNoInitSqlIsConfigured(CapturedOutput output) {
        try (HikariDataSource dataSource = new HikariDataSource()) {
            postProcess(dataSource);

            assertThat(dataSource.getConnectionInitSql())
                    .isEqualTo(TrigramThresholdAutoConfiguration.CONNECTION_INIT_SQL);
            assertThat(output).doesNotContain("Trigramm-Schwelle");
        }
    }

    @Test
    void keepsExplicitInitSqlAndWarns(CapturedOutput output) {
        try (HikariDataSource dataSource = new HikariDataSource()) {
            dataSource.setConnectionInitSql("SET statement_timeout = '5s'");

            postProcess(dataSource);

            assertThat(dataSource.getConnectionInitSql()).isEqualTo("SET statement_timeout = '5s'");
            assertThat(output).contains("WARN").contains("Pool 'dataSource'").contains("Trigramm-Schwelle 0.4");
        }
    }

    private static void postProcess(HikariDataSource dataSource) {
        TrigramThresholdAutoConfiguration.trigramThresholdPostProcessor()
                .postProcessAfterInitialization(dataSource, "dataSource");
    }
}
