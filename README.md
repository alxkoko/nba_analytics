# NBA Player Analytics

Personal/educational web app: search NBA players, view season and game-by-game stats, and answer questions like “how often did he score over X points?” with probabilities and charts.

## Stack

- **Backend:** Spring Boot (REST API)
- **Frontend:** React (Vite)
- **Database:** PostgreSQL
- **Data ingestion:** Python (nba_api) → PostgreSQL

## Repo layout

| Path | Purpose |
|------|--------|
| `docs/ARCHITECTURE.md` | High-level architecture, data source, MVP, first-week plan |
| `database/schema.sql` | PostgreSQL schema (players, player_game_logs, player_season_stats) |
| `ingestion/` | Python scripts: ingest from nba_api, over/under analytics |

## Quick start

1. **Ingest:** Create DB, run `database/schema.sql`, set `DATABASE_URL` in `ingestion/.env`, then `cd ingestion && pip install -r requirements.txt && python ingest.py`.
2. **Backend:** `cd backend && mvn spring-boot:run` (set DB password in `application.properties` or env).
3. **Frontend:** `cd frontend && npm install && npm run dev` — open http://localhost:5173.

## Deployment

See **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** for options (VPS, PaaS, Docker) and step-by-step instructions.

## Data source

[nba_api](https://github.com/swar/nba_api) (NBA.com; no API key). Use `--delay 0.6` or higher to avoid rate limits.
