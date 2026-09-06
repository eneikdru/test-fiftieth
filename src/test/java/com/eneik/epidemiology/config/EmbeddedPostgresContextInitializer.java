package com.eneik.epidemiology.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedPostgresContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static EmbeddedPostgres embeddedPostgres;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        synchronized (EmbeddedPostgresContextInitializer.class) {
            if (embeddedPostgres == null) {
                try {
                    embeddedPostgres = EmbeddedPostgres.builder().start();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to start Embedded Postgres", e);
                }
            }
        }

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", embeddedPostgres.getJdbcUrl("postgres", "postgres"));
        props.put("spring.datasource.username", "postgres");
        props.put("spring.datasource.password", "postgres");
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        applicationContext.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("embeddedPostgresProps", props)
        );
    }
}
