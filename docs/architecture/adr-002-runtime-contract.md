# ADR-002: Runtime Contract and Datastore Standardization

## Status
Accepted

## Context
Prior to this contract, runtime artifacts in the repository disagreed on the underlying datastore engine. `docker-compose.yml` provided PostgreSQL 15, while `src/main/resources/application.properties` defaulted to H2 in-memory database and `pom.xml` omitted the PostgreSQL JDBC driver dependency. This created environment drift where database migrations and queries tested against H2 could fail or behave differently when executed against PostgreSQL in delivery environments.

## Decision
1. **Canonical Datastore Engine**: PostgreSQL 15 is the single canonical datastore engine across all environments (runtime delivery, local development, and CI/testing).
2. **Build Manifest Contract (`pom.xml`)**: Must include the PostgreSQL JDBC driver (`org.postgresql:postgresql`) and Flyway migration support (`org.flywaydb:flyway-core`).
3. **Application Configuration Contract (`application.properties`)**: Must default to PostgreSQL using environment variable placeholders aligned with Compose:
   - `spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:epidemiology_db}`
   - `spring.datasource.driver-class-name=org.postgresql.Driver`
   - `spring.datasource.username=${POSTGRES_USER:postgres}`
   - `spring.datasource.password=${POSTGRES_PASSWORD:postgres}`
   - `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`
4. **Flyway Migrations**: All migration DDL scripts under `src/main/resources/db/migration` must use valid PostgreSQL-native SQL dialect (e.g. `BIGSERIAL`, `TEXT`, `TIMESTAMPTZ`).

## Consequences
- Eliminates configuration drift between local development, testing, and containerized delivery.
- All database queries and Flyway migrations are validated directly against PostgreSQL.
