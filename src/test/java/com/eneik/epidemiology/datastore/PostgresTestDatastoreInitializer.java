package com.eneik.epidemiology.datastore;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PostgresTestDatastoreInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static EmbeddedPostgres embeddedPostgres;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        synchronized (PostgresTestDatastoreInitializer.class) {
            if (embeddedPostgres == null) {
                try {
                    embeddedPostgres = EmbeddedPostgres.builder().start();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to start Embedded PostgreSQL for test datastore", e);
                }
            }
        }

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", embeddedPostgres.getJdbcUrl("postgres", "postgres"));
        props.put("spring.datasource.username", "postgres");
        props.put("spring.datasource.password", "postgres");
        props.put("spring.datasource.driverClassName", "org.postgresql.Driver");
        props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("spring.test.database.replace", "NONE");

        applicationContext.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("postgresTestDatastoreProps", props)
        );
    }
}
