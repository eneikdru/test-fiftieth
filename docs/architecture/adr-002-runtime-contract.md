# ADR-002: Datastore Runtime Contract Alignment

## Status
Accepted

## Context
Prior runtime artifacts in the repository disagreed on the relational datastore engine:
- `docker-compose.yml` configured PostgreSQL 15 (`postgres:15-alpine`).
- `src/main/resources/application.properties` defaulted to H2 in-memory database.
- `pom.xml` declared H2 runtime dependency but lacked the PostgreSQL JDBC driver.

This inconsistency risks SQL dialect mismatches passing local gates while failing in containerized/production delivery.

## Decision
PostgreSQL 15 is defined as the single source of truth for the primary relational datastore engine across all application runtime environments (development, test execution, containerized delivery, and production).

All repository configuration artifacts must be derived from and synchronized with this decision:
1. `docker-compose.yml` provides PostgreSQL 15 (`postgres:15-alpine`).
2. `pom.xml` includes `org.postgresql:postgresql` as runtime dependency.
3. `src/main/resources/application.properties` configures PostgreSQL defaults using environment variables with PostgreSQL fallbacks:
   - `spring.datasource.url`: `jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${POSTGRES_DB:epidemiology_db}`
   - `spring.datasource.driver-class-name`: `org.postgresql.Driver`
   - `spring.jpa.database-platform`: `org.hibernate.dialect.PostgreSQLDialect`

## Consequences
- Engine consistency across dev, test, and containerized deployment.
- Database schema migrations (Flyway) and JPA queries execute against PostgreSQL semantics.
