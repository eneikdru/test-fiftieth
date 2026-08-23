package com.eneik.epidemiology;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.io.IOException;

public class PostgresTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static EmbeddedPostgres embeddedPostgres;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        synchronized (PostgresTestContainerInitializer.class) {
            if (embeddedPostgres == null) {
                try {
                    embeddedPostgres = EmbeddedPostgres.builder().start();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to start embedded Postgres database", e);
                }
            }
        }

        String jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.datasource.url=" + jdbcUrl,
                "spring.datasource.driverClassName=org.postgresql.Driver",
                "spring.datasource.username=postgres",
                "spring.datasource.password=postgres",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.flyway.enabled=true"
        );
    }
}
