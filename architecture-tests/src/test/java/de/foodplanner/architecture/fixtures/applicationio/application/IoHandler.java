package de.foodplanner.architecture.fixtures.applicationio.application;

import jakarta.persistence.EntityManager;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.sql.Connection;
import javax.net.ssl.HttpsURLConnection;
import javax.sql.DataSource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

public class IoHandler {
    HttpClient httpClient;
    HttpURLConnection httpUrlConnection;
    HttpsURLConnection httpsUrlConnection;
    Connection connection;
    DataSource dataSource;
    RestClient restClient;
    HttpHeaders httpHeaders;
    JdbcClient jdbcClient;
    EntityManager entityManager;
}
