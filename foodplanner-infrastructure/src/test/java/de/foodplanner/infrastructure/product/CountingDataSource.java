package de.foodplanner.infrastructure.product;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Zählt vorbereitete Anweisungen, damit Tests eine feste Zahl an Abfragen festhalten können. Die Verbindung kommt aus
 * der laufenden Testtransaktion, damit die Testdaten sichtbar sind; {@code close()} gibt sie nur an Spring zurück.
 */
final class CountingDataSource {

    private CountingDataSource() {
    }

    static DataSource of(DataSource dataSource, AtomicInteger statements) {
        return new DelegatingDataSource(dataSource) {
            @Override
            public Connection getConnection() {
                Connection target = DataSourceUtils.getConnection(dataSource);
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                            if (method.getName().equals("close")) {
                                DataSourceUtils.releaseConnection(target, dataSource);
                                return null;
                            }
                            if (method.getName().equals("prepareStatement")
                                    || method.getName().equals("createStatement")) {
                                statements.incrementAndGet();
                            }
                            try {
                                return method.invoke(target, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
            }
        };
    }
}
