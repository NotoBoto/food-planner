package de.foodplanner.infrastructure.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Setzt die Trigramm-Schwelle der lexikalischen Suche (§5.5) über {@code connectionInitSql} des HikariCP-Pools. Die
 * Einstellung gehört zum Suchadapter, nicht zu jeder Anwendung: Jede Anwendung mit diesem Modul auf dem Klassenpfad
 * (API, Ingestion, Tests) bekommt sie, ohne sie in ihre {@code application.properties} zu kopieren.
 *
 * <p>Ein ausdrücklich gesetztes {@code spring.datasource.hikari.connection-init-sql} bleibt unangetastet. Die Suche
 * läuft dann mit dem Datenbank-Default 0,6 und findet weniger; deshalb wird beim Start eine Warnung geloggt.
 */
@AutoConfiguration
@ConditionalOnClass(HikariDataSource.class)
public class TrigramThresholdAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TrigramThresholdAutoConfiguration.class);

    /** Schwelle für {@code <%} (§5.5); der Datenbank-Default 0,6 ist für Wortteile zu streng. */
    static final String CONNECTION_INIT_SQL = "SET pg_trgm.word_similarity_threshold = 0.4";

    @Bean
    static BeanPostProcessor trigramThresholdPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource dataSource) {
                    if (dataSource.getConnectionInitSql() == null) {
                        dataSource.setConnectionInitSql(CONNECTION_INIT_SQL);
                    } else {
                        LOG.warn("Pool '{}' hat bereits connectionInitSql; die Trigramm-Schwelle 0.4 wird nicht gesetzt."
                                + " Die Suche nutzt dann den Datenbank-Default 0.6 und findet weniger Treffer, sofern"
                                + " das gesetzte SQL die Schwelle nicht selbst setzt.", beanName);
                    }
                }
                return bean;
            }
        };
    }
}
