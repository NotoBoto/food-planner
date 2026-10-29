package de.foodplanner.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL mit pgvector für alle Datenbanktests. Der Container gehört zum Spring-Kontext; da Spring
 * Testkontexte zwischenspeichert, teilen sich alle Tests mit derselben Konfiguration einen Container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    /** Muss mit dem Image des Service {@code db} in {@code docker-compose.yml} übereinstimmen. */
    public static final String IMAGE = "pgvector/pgvector:0.8.7-pg18-trixie";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"));
    }
}
