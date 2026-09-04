package com.eneik.epidemiology.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PostgresTestDatastoreVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Given the application runs in test, When inspecting datasource metadata, Then it runs against PostgreSQL engine without mismatch errors")
    void testDatastoreIsPostgresql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            assertNotNull(databaseProductName, "Database product name should not be null");
            assertTrue(
                databaseProductName.toLowerCase().contains("postgresql"),
                "Expected database product to be PostgreSQL, but was: " + databaseProductName
            );
        }
    }

    @Test
    @DisplayName("Given database query is executed against test datastore, When UserService queries user repository, Then operation succeeds against real Postgres")
    void testDatabaseQueryExecutesAgainstPostgres() {
        User user = new User("test_postgres_user", "hashed_pass", "RESEARCHER");
        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertTrue(userRepository.findByUsername("test_postgres_user").isPresent());
    }
}
