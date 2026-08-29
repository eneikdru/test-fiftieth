package com.eneik.epidemiology.datastore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UnifiedDatastoreVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Given test suite execution, When connected to datastore, Then engine is PostgreSQL and not H2")
    void testDatastoreIsPostgreSQL() throws SQLException {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            assertNotNull(databaseProductName, "Database product name must not be null");

            assertTrue(databaseProductName.toLowerCase().contains("postgresql"),
                    "Database product name should be PostgreSQL but was: " + databaseProductName);
            assertFalse(databaseProductName.toLowerCase().contains("h2"),
                    "Database product name must not be H2");
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Test
    @DisplayName("Given database schema metadata, When inspecting data types, Then no H2 specific types like VARBINARY leak")
    void testNoH2SpecificDataTypesLeak() throws SQLException {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            DatabaseMetaData metaData = conn.getMetaData();

            // 1. Verify typeInfo does not contain H2-specific type names
            ResultSet typeInfo = metaData.getTypeInfo();
            boolean h2SpecificTypeInTypeInfo = false;
            while (typeInfo.next()) {
                String typeName = typeInfo.getString("TYPE_NAME");
                if ("CHARACTER VARYING SELECTIVE".equalsIgnoreCase(typeName) || "VARBINARY".equalsIgnoreCase(typeName)) {
                    h2SpecificTypeInTypeInfo = true;
                    break;
                }
            }
            assertFalse(h2SpecificTypeInTypeInfo, "H2-specific data type names (such as VARBINARY) must not leak in PostgreSQL type info");

            // 2. Verify table columns in schema do not use VARBINARY as type name
            ResultSet columns = metaData.getColumns(null, "public", "%", "%");
            boolean varbinaryColumnFound = false;
            while (columns.next()) {
                String typeName = columns.getString("TYPE_NAME");
                if ("VARBINARY".equalsIgnoreCase(typeName)) {
                    varbinaryColumnFound = true;
                    break;
                }
            }
            assertFalse(varbinaryColumnFound, "No schema column should use H2 VARBINARY type name; PostgreSQL bytea must be used instead");
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }
}
