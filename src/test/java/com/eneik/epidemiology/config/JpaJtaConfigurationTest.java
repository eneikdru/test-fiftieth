package com.eneik.epidemiology.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class JpaJtaConfigurationTest {

    @Test
    @DisplayName("Given main application properties, Then spring.jpa.properties.hibernate.transaction.jta.platform is configured to NoJtaPlatform")
    void testSpringJpaJtaPlatformConfiguredInMainProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = JpaJtaConfigurationTest.class.getResourceAsStream("/application.properties")) {
            assertThat(is).isNotNull();
            props.load(is);
        }
        assertThat(props.getProperty("spring.jpa.properties.hibernate.transaction.jta.platform"))
                .isEqualTo("org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform");
    }

    @Test
    @DisplayName("Given main application properties, Then hibernate.transaction.jta.platform is configured to NoJtaPlatform")
    void testHibernateJtaPlatformConfiguredInMainProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = JpaJtaConfigurationTest.class.getResourceAsStream("/application.properties")) {
            assertThat(is).isNotNull();
            props.load(is);
        }
        assertThat(props.getProperty("hibernate.transaction.jta.platform"))
                .isEqualTo("org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform");
    }
}
