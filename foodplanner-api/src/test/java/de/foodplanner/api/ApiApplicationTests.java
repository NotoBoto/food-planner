package de.foodplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import de.foodplanner.infrastructure.PostgresTestConfiguration;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class ApiApplicationTests {
    @Autowired
    JdbcClient jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayMigratesOnStartup() {
        assertThat(jdbc.sql("SELECT version FROM flyway_schema_history WHERE success")
                .query(String.class).list())
                .contains("1");
    }
}
