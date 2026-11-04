package de.foodplanner.infrastructure.product;

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
 * <p>Ein ausdrücklich gesetztes {@code spring.datasource.hikari.connection-init-sql} bleibt unangetastet.
 */
@AutoConfiguration
@ConditionalOnClass(HikariDataSource.class)
public class TrigramThresholdAutoConfiguration {

    /** Schwelle für {@code <%} (§5.5); der Datenbank-Default 0,6 ist für Wortteile zu streng. */
    static final String CONNECTION_INIT_SQL = "SET pg_trgm.word_similarity_threshold = 0.4";

    @Bean
    static BeanPostProcessor trigramThresholdPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource dataSource && dataSource.getConnectionInitSql() == null) {
                    dataSource.setConnectionInitSql(CONNECTION_INIT_SQL);
                }
                return bean;
            }
        };
    }
}
