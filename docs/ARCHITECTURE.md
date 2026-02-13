# NBA Player Analytics — Architecture & Plan

## 1. High-Level Architecture

```
┌─────────────────┐     REST      ┌─────────────────┐     JDBC      ┌─────────────────┐
│  React SPA      │ ◄────────────► │  Spring Boot    │ ◄────────────► │  PostgreSQL     │
│  (search,       │   JSON        │  (REST API,     │               │  (players,      │
│   charts,       │               │   analytics)     │               │   games, stats) │
│   probabilities)│               │                  │               │                 │
└─────────────────┘               └────────┬────────┘               └─────────────────┘
                                            │
                                            │ (optional: call Python or run SQL)
                                            ▼
                                   ┌─────────────────┐
                                   │  Python         │
                                   │  (ingestion,    │
                                   │   one-off       │
                                   │   analytics)    │
                                   └─────────────────┘
```

- **Frontend (React):** Search players, pick season, view game log + season stats, over/under probabilities, simple charts (e.g. points last N games, vs opponent).
- **Backend (Spring Boot):** Serves REST API; reads/writes PostgreSQL; can host probability logic (reimplemented in Java) or delegate to precomputed tables/views filled by Python.
- **Database (PostgreSQL):** Single source of truth for players, games, and box-score stats.
- **Python:** Used for **data ingestion** (fetch from NBA source, normalize, load into DB). Optionally for heavier analytics; MVP can keep probability math in Spring Boot for simplicity.

**Why this split:** You’re strong in Java/React; Python is only required where it adds value (ingestion, exploratory analysis). Keeping the app “Spring Boot + React + Postgres” keeps the stack familiar; Python runs as a separate process (cron or manual) to populate the DB.

---

## 2. Recommended Data Source: **nba_api** (Python)

| Option | Pros | Cons |
|--------|------|------|
| **nba_api** (NBA.com) | No API key, player + game logs + season stats, well-documented, Python-native | Unofficial; rate limits; can break if NBA.com changes |
| Basketball-Reference scraping | Very rich (splits, opponent, home/away) | Scraping is fragile; ToS/ethics; more code |
| balldontlie.io | Official-style REST API | Limited history; rate limits; less game-level detail |

**Recommendation: nba_api.**

- Fits “Python for ingestion” and “new to scraping”: it’s an API client, not raw scraping.
- Covers MVP: player search, season totals, game-by-game logs.
- You can add Basketball-Reference or other sources later for “vs opponent” or “after injury” if needed.

**Pitfalls to avoid:**

- **Rate limits:** Use a small delay between requests (e.g. 0.6–1 s); batch by player or season.
- **Data quality:** Check for nulls and duplicate games; use `game_id` (or equivalent) as idempotency key.
- **Season format:** NBA.com uses e.g. `"2024-25"`; store consistently (e.g. `season` text or start year).

---

## 3. MVP Feature Set

**In scope for MVP:**

1. **Player search:** By name (fuzzy), list of matching players with id and team.
2. **Season stats:** For a chosen player + season: GP, PPG, RPG, APG, etc. (from your DB).
3. **Game log:** Table of games (date, opponent, home/away, points, rebounds, assists, etc.).
4. **Over/under points:** “How often did he score over X points this season?” — percentage and count (e.g. “12 of 41 games, 29.3%”).
5. **Last N games:** Filter or highlight last N games and show average (e.g. “Last 5: 24.2 PPG”).
6. **Simple charts:** One chart: points per game (e.g. last 20 or full season bar/line).

**Explicitly out of scope for MVP (phase 2):**

- “Vs specific opponent” (needs opponent in schema + UI filter).
- “Coming off injury” (needs DNP/games missed logic and definitions).
- Trend curves and advanced models.

---

## 4. First Week Implementation Steps

| Day | Focus | Deliverable |
|-----|--------|--------------|
| 1 | Schema + ingestion | PostgreSQL schema applied; Python script that pulls one player’s game log for one season and inserts into DB. |
| 2 | Ingestion robustness | Idempotency (no duplicate games); ingest multiple players/seasons; basic logging and error handling. |
| 3 | Spring Boot API | Project with JPA entities mirroring schema; REST: `GET /api/players?q=`, `GET /api/players/{id}/seasons`, `GET /api/players/{id}/games?season=`, `GET /api/players/{id}/stats?season=`. |
| 4 | Over/under + last N | Backend: endpoints or query params for “games over X points” and “last N games” stats; probability = count over X / total games. |
| 5 | React MVP | Search box → player list; select player + season → game log table + season summary; over/under block (input X, show % and count); “Last N” selector; one chart (e.g. points last 20 games). |

Each step is minimal but extensible: e.g. “last N” is a query parameter that can later drive “after injury” (N = games since return).

---

## 5. Next Sections (Implemented in Repo)

- **Database schema:** `database/schema.sql` — players, games, game_stats (or equivalent), seasons.
- **Python ingestion:** `ingestion/` — script using nba_api to fetch and upsert into PostgreSQL.
- **Probability logic:** Over/under as “count(games where pts >= X) / count(games)”;
  - Implemented in Python for ingestion/analysis and mirrored in Spring Boot for the API.

---

## 6. Tech Stack Summary

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Backend | Spring Boot 3.x, Java 17+ | Your stack; JPA, validation, Actuator. |
| Frontend | React 18, Vite | Fast; fetch from Spring Boot REST. |
| Database | PostgreSQL 15+ | Your preference; JSONB optional for raw payloads. |
| Ingestion | Python 3.10+, nba_api | Best data source fit; minimal code. |
| Charts | Recharts or Chart.js | Simple, React-friendly. |

All of this is actionable with the schema, ingestion script, and probability logic described next.
