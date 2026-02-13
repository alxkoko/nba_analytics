-- NBA Player Analytics — PostgreSQL Schema
-- Run once to create tables. Safe to re-run (IF NOT EXISTS / ON CONFLICT used where applicable).

-- Players (from NBA.com / nba_api common player list)
CREATE TABLE IF NOT EXISTS players (
    id              BIGSERIAL PRIMARY KEY,
    nba_player_id   BIGINT NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    first_name      VARCHAR(128),
    last_name       VARCHAR(128),
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_players_full_name ON players (LOWER(full_name));
CREATE INDEX IF NOT EXISTS idx_players_nba_id ON players (nba_player_id);

-- One row per player per game (box score line). game_id is NBA’s unique game identifier.
CREATE TABLE IF NOT EXISTS player_game_logs (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    nba_game_id     VARCHAR(32) NOT NULL,
    game_date       DATE NOT NULL,
    season          VARCHAR(9) NOT NULL,  -- e.g. '2024-25'
    matchup         VARCHAR(32),           -- e.g. 'LAL vs. BOS' or 'LAL @ BOS'
    home_away       CHAR(1),               -- 'H' or 'A' (derived from matchup if needed)
    team_abbr       VARCHAR(8),
    opponent_abbr   VARCHAR(8),
    wl              CHAR(1),               -- 'W' or 'L'
    min_played      INTEGER,               -- minutes (can be NULL if DNP)
    pts             INTEGER NOT NULL DEFAULT 0,
    reb             INTEGER NOT NULL DEFAULT 0,
    ast             INTEGER NOT NULL DEFAULT 0,
    stl             INTEGER NOT NULL DEFAULT 0,
    blk             INTEGER NOT NULL DEFAULT 0,
    tov             INTEGER NOT NULL DEFAULT 0,
    fgm             INTEGER NOT NULL DEFAULT 0,
    fga             INTEGER NOT NULL DEFAULT 0,
    fg3m            INTEGER NOT NULL DEFAULT 0,
    fg3a            INTEGER NOT NULL DEFAULT 0,
    ftm             INTEGER NOT NULL DEFAULT 0,
    fta             INTEGER NOT NULL DEFAULT 0,
    oreb            INTEGER NOT NULL DEFAULT 0,
    dreb            INTEGER NOT NULL DEFAULT 0,
    pf              INTEGER NOT NULL DEFAULT 0,
    plus_minus      INTEGER,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (player_id, nba_game_id)
);

CREATE INDEX IF NOT EXISTS idx_pgl_player_season ON player_game_logs (player_id, season);
CREATE INDEX IF NOT EXISTS idx_pgl_player_date ON player_game_logs (player_id, game_date DESC);
CREATE INDEX IF NOT EXISTS idx_pgl_opponent ON player_game_logs (player_id, opponent_abbr);

-- Optional: cache season aggregates per player (refreshed by ingestion or a scheduled job)
CREATE TABLE IF NOT EXISTS player_season_stats (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    season          VARCHAR(9) NOT NULL,
    games_played    INTEGER NOT NULL DEFAULT 0,
    pts_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    reb_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    ast_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    stl_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    blk_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    tov_avg         NUMERIC(5,2) NOT NULL DEFAULT 0,
    fg_pct          NUMERIC(5,3),
    fg3_pct         NUMERIC(5,3),
    ft_pct          NUMERIC(5,3),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (player_id, season)
);

CREATE INDEX IF NOT EXISTS idx_pss_player ON player_season_stats (player_id);

-- Trigger to keep updated_at current (optional)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS players_updated_at ON players;
CREATE TRIGGER players_updated_at
    BEFORE UPDATE ON players
    FOR EACH ROW EXECUTE PROCEDURE set_updated_at();

DROP TRIGGER IF EXISTS player_game_logs_updated_at ON player_game_logs;
CREATE TRIGGER player_game_logs_updated_at
    BEFORE UPDATE ON player_game_logs
    FOR EACH ROW EXECUTE PROCEDURE set_updated_at();
