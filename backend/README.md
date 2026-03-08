# NBA Stats API (Spring Boot)

REST API for players, game logs, season stats, and over/under probabilities.

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL with `nba_stats` database and schema applied (see repo root and `database/schema.sql`)

## Configuration

Edit `src/main/resources/application.properties` or set env vars:

- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/nba_stats`)
- `SPRING_DATASOURCE_USERNAME` (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (set your password)

## Build and run

```bash
cd backend
mvn spring-boot:run
```

Or build a JAR:

```bash
mvn clean package
java -jar target/nba-stats-api-0.0.1-SNAPSHOT.jar
```

## Deploying on Railway (500MB free tier)

The app can hit the 500MB container limit because the JVM uses a large default heap. To avoid out-of-memory crashes:

1. **Set the service Root Directory** to `backend` in Railway so the `Procfile` is used. The Procfile starts the JAR with `-Xmx256m -Xms128m -XX:MaxMetaspaceSize=64m` to keep memory under the limit.

2. **Optional:** In Railway → your service → Variables, set `JAVA_OPTS` if you want different limits (e.g. `-Xmx300m -Xms128m`). If you use a custom start command, pass these flags to `java`.

3. **application.properties** already sets a small HikariCP pool (`maximum-pool-size=3`) and Tomcat thread cap (`server.tomcat.threads.max=25`) to reduce memory use.

4. Ensure `server.port=${PORT:8080}` and `server.address=0.0.0.0` (included in the example) so the app binds to Railway’s `PORT` and is reachable.

API base: **http://localhost:8080**

## Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/season/current` | Current NBA season (e.g. `{"season":"2024-25"}`) |
| GET | `/api/season/list?count=5` | List of seasons (current + past) |
| GET | `/api/players?q=LeBron` | Search players by name |
| GET | `/api/players/{id}` | Get one player |
| GET | `/api/players/{id}/games?season=2024-25` | Game log for season |
| GET | `/api/players/{id}/stats?season=2024-25` | Season averages (PPG, RPG, etc.) |
| GET | `/api/players/{id}/over-under?season=2024-25&stat=pts&threshold=25&lastN=10` | Over/under probability (optional `lastN`) |

## Example

```bash
curl "http://localhost:8080/api/players?q=LeBron"
curl "http://localhost:8080/api/players/1/games?season=2024-25"
curl "http://localhost:8080/api/players/1/over-under?season=2024-25&stat=pts&threshold=25"
```
