# NBA Data Ingestion & Analytics (Python)

## Setup

```bash
cd ingestion
python -m venv .venv
.venv\Scripts\activate   # Windows
pip install -r requirements.txt
cp .env.example .env     # set DATABASE_URL
```

Create the DB and apply schema:

```bash
psql -U postgres -c "CREATE DATABASE nba_stats;"
psql -U postgres -d nba_stats -f ../database/schema.sql
```

## Ingestion

Default season is **current** (Oct–June = that year’s season; July–Sep = previous). Override with `--season` if needed.

```bash
python ingest.py                           # sample players, current season
python ingest.py --player "LeBron"         # by name
python ingest.py --player-id 2544         # by NBA id
python ingest.py --season 2024-25         # override season (default: current)
python ingest.py --delay 1.0 --dry-run    # slower, no DB write
```

Re-run periodically to pull new games for the current season.

## Over/Under Probability (analytics.py)

- **In Python:** Use `over_under_probability(values, threshold, last_n=...)` on a list of point totals, or `get_over_under_from_db(conn, player_id, season, "pts", 25.5, last_n=10)` to read from DB.
- **In Spring Boot:** Same logic in SQL, e.g.:

```sql
-- Probability over X points this season
SELECT
  COUNT(*) FILTER (WHERE pts >= :threshold) AS games_over,
  COUNT(*) AS total_games,
  ROUND(COUNT(*) FILTER (WHERE pts >= :threshold)::numeric / NULLIF(COUNT(*), 0), 4) AS probability_over
FROM player_game_logs
WHERE player_id = :playerId AND season = :season
  AND (:lastN IS NULL OR id IN (
    SELECT id FROM player_game_logs
    WHERE player_id = :playerId AND season = :season
    ORDER BY game_date DESC LIMIT :lastN
  ));
```

Or in Java: load game log rows, then `gamesOver = games.stream().filter(g -> g.getPts() >= threshold).count(); probabilityOver = (double) gamesOver / games.size();`

## Pitfalls

- **Rate limits:** Use `--delay 0.6` or higher; avoid bulk runs without throttling.
- **Season format:** Store as `YYYY-YY` (e.g. `2024-25`). nba_api may return `SEASON_ID` like `22024`; the script normalizes.
- **Idempotency:** Upserts use `(player_id, nba_game_id)`; re-running is safe.
