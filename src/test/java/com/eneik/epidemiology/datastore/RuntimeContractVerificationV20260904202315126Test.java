package com.eneik.epidemiology.datastore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class RuntimeContractVerificationV20260904202315126Test {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given applied migration V20260904202315126 resolved locally, When application starts, Then Spring context initializes successfully without Flyway validation errors")
    void testFlywayValidationAndContextInitialization() {
        assertNotNull(dataSource, "DataSource bean should be initialized after Flyway migrations complete");
    }
}
