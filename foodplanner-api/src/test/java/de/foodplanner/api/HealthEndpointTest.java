package de.foodplanner.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

/** §5.1: Actuator-Health unter {@code /health}, sonst ist kein Actuator-Endpunkt erreichbar. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class HealthEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HealthContributorRegistry healthContributorRegistry;

    @Test
    void healthIsUpAtRootPath() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
    }

    /**
     * AC: Datenbank nicht erreichbar → 503 {@code DOWN}. Statt die geteilte Datenbank abzuschalten, wird der echte
     * Datenbank-Indikator {@code db} durch einen ersetzt, der {@code DOWN} meldet. Fiele {@code db} aus der
     * Health-Gruppe oder würde {@code DOWN} auf 200 abgebildet, schlüge der Test fehl.
     */
    @Test
    void healthIsDownWith503WhenDatabaseIsDown() throws Exception {
        HealthContributor db = healthContributorRegistry.unregisterContributor("db");
        assertThat(db).as("Datenbank-Indikator db ist registriert").isNotNull();
        try {
            healthContributorRegistry.registerContributor("db", (HealthIndicator) () -> Health.down().build());

            mockMvc.perform(get("/health"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json("{\"status\":\"DOWN\"}", JsonCompareMode.STRICT));
        } finally {
            healthContributorRegistry.unregisterContributor("db");
            healthContributorRegistry.registerContributor("db", db);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator", "/actuator/health", "/info", "/env", "/beans", "/api/v1/health",
            "/health/liveness", "/health/readiness", "/"})
    void otherActuatorPathsAreNotExposed(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
