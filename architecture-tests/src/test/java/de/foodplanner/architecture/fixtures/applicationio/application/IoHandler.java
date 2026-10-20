package de.foodplanner.architecture.fixtures.applicationio.application;

import jakarta.persistence.EntityManager;
import javax.net.ssl.HttpsURLConnection;
import javax.sql.DataSource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

public class IoHandler {
    HttpsURLConnection httpsUrlConnection;
    DataSource dataSource;
    RestClient restClient;
    HttpHeaders httpHeaders;
    JdbcClient jdbcClient;
    EntityManager entityManager;
}
