# KTB Final Team Backend

Spring Boot backend project built with Java 25 and Gradle.

## Run

```bash
./gradlew bootRun
```

The default local database is an in-memory H2 database. Configure another database with:

```bash
DB_URL=jdbc:mysql://localhost:3306/ktb \
DB_USERNAME=ktb \
DB_PASSWORD=change-me \
DB_DRIVER=com.mysql.cj.jdbc.Driver \
JPA_DDL_AUTO=update \
./gradlew bootRun
```

## Test

```bash
./gradlew test
```
