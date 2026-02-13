"""
NBA player and game-log ingestion into PostgreSQL using nba_api.
Run after applying database/schema.sql. Idempotent: safe to re-run (upserts by nba_player_id, player_id+game_id).

Usage:
  python ingest.py                          # ingest a few sample players for current season
  python ingest.py --player "LeBron"        # find and ingest one player by name
  python ingest.py --player-id 2544         # ingest by NBA player ID (LeBron)
  python ingest.py --season 2024-25         # override season (default: current)
  python ingest.py --delay 1.0               # seconds between API calls (default 0.6)
"""

import argparse
import concurrent.futures
import logging
import os
import time
from contextlib import contextmanager
from datetime import date

import psycopg2
from psycopg2.extras import execute_values
from dotenv import load_dotenv

load_dotenv()

try:
    from nba_api.stats.endpoints import commonplayerinfo, playergamelog
    from nba_api.stats.static import players as nba_players_static
except ImportError:
    raise SystemExit("Install deps: pip install nba_api psycopg2-binary python-dotenv")

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

REQUEST_DELAY = 0.6

# Default player names when no --player or --player-id (resolved to NBA IDs at runtime)
DEFAULT_PLAYER_NAMES = [
    "Nikola Jokic",
    "Shai Gilgeous-Alexander",
    "Luka Doncic",
    "Giannis Antetokounmpo",
    "Victor Wembanyama",
    "Anthony Edwards",
    "Stephen Curry",
    "Donovan Mitchell",
    "Cade Cunningham",
    "Kawhi Leonard",
    "Jalen Brunson",
    "Jaylen Brown",
    "Kevin Durant",
    "Tyrese Maxey",
    "Devin Booker",
    "Jamal Murray",
    "Deni Avdija",
    "Alperen Sengun",
    "Chet Holmgren",
    "Jalen Johnson",
    "LeBron James",
    "Evan Mobley",
    "Scottie Barnes",
    "James Harden",
    "Jalen Williams",
    "Pascal Siakam",
    "Karl-Anthony Towns",
    "Lauri Markkanen",
    "Bam Adebayo",
    "Anthony Davis",
    "Jalen Duren",
    "Joel Embiid",
    "Franz Wagner",
    "Julius Randle",
    "De'Aaron Fox",
    "Paolo Banchero",
    "Austin Reaves",
    "Derrick White",
    "Rudy Gobert",
    "Aaron Gordon",
    "Amen Thompson",
    "OG Anunoby",
    "Jaren Jackson Jr.",
    "Norm Powell",
    "LaMelo Ball",
    "Domantas Sabonis",
    "Michael Porter Jr.",
    "Stephon Castle",
    "Trey Murphy III",
    "Ivica Zubac",
    "Brandon Ingram",
    "Desmond Bane",
    "Jaden McDaniels",
    "Mikal Bridges",
    "Keyonte George",
    "Trae Young",
    "Cooper Flagg",
    "Zion Williamson",
    "Darius Garland",
    "Josh Giddey",
    "Jalen Suggs",
    "Tyler Herro",
    "Jrue Holiday",
    "Dillon Brooks",
    "Ja Morant",
    "Isaiah Hartenstein",
    "Andrew Nembhard",
    "Jarrett Allen",
    "Naz Reid",
    "Peyton Watson",
    "Alex Caruso",
    "Draymond Green",
    "Nickeil Alexander-Walker",
    "Ausar Thompson",
    "RJ Barrett",
    "Coby White",
    "Zach LaVine",
    "Josh Hart",
    "Alex Sarr",
    "Brandon Miller",
    "Dyson Daniels",
    "DeMar DeRozan",
    "Luguentz Dort",
    "Payton Pritchard",
    "Anthony Black",
    "Onyeka Okongwu",
    "Jaime Jaquez Jr.",
    "Reed Sheppard",
    "Paul George",
    "Tari Eason",
    "Cason Wallace",
    "Devin Vassell",
    "Isaiah Stewart",
    "Toumani Camara",
    "Cam Johnson",
    "Immanuel Quickley",
    "Shaedon Sharpe",
    "Ajay Mitchell",
    "Kon Knueppel",
    "VJ Edgecombe",
]
# Names not in nba_api (e.g. future draft) will be skipped with a warning.


def get_current_season():
    """NBA season: Oct–June = e.g. 2024-25; July–Sep = previous season (2024-25 until next Oct)."""
    today = date.today()
    year = today.year
    month = today.month
    # Season starts in October (month 10)
    start_year = year if month >= 10 else year - 1
    end_yy = str((start_year + 1) % 100).zfill(2)
    return f"{start_year}-{end_yy}"


def get_db():
    url = os.environ.get("DATABASE_URL")
    if not url:
        raise SystemExit("Set DATABASE_URL (e.g. in .env). See .env.example.")
    return psycopg2.connect(url)


@contextmanager
def db_connection():
    conn = get_db()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def parse_matchup(matchup: str | None):
    """Parse MATCHUP like 'LAL vs. BOS' or 'LAL @ BOS' -> (team_abbr, opponent_abbr, home_away)."""
    if not matchup or not isinstance(matchup, str):
        return None, None, None
    # e.g. "LAL vs. BOS" (home) or "LAL @ BOS" (away)
    parts = matchup.strip().split()
    if len(parts) < 3:
        return None, None, None
    team_abbr = parts[0]
    opp_abbr = parts[-1]
    home_away = "H" if "vs." in matchup else "A"
    return team_abbr, opp_abbr, home_away


def ensure_player(conn, nba_player_id: int, full_name: str, first_name: str | None = None, last_name: str | None = None):
    """Insert or get player by nba_player_id. Returns db_id."""
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO players (nba_player_id, full_name, first_name, last_name)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (nba_player_id) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                updated_at = NOW()
            RETURNING id
            """,
            (nba_player_id, full_name, first_name or "", last_name or ""),
        )
        row = cur.fetchone()
    return row[0]


def _normalize_season_id(raw):
    """Convert NBA SEASON_ID (e.g. '22025') to '2025-26'."""
    if raw is None or raw == "":
        return ""
    raw = str(raw).strip()
    if len(raw) >= 5 and raw.isdigit():
        start_year = int(raw[1:5])
        return f"{start_year}-{str((start_year + 1) % 100).zfill(2)}"
    return raw


def upsert_game_logs(conn, player_id: int, rows: list[dict]):
    """Insert or update player_game_logs. Each row: nba_game_id, game_date, season, matchup, wl, min_played, pts, reb, ast, stl, blk, tov, fgm, fga, fg3m, fg3a, ftm, fta, oreb, dreb, pf, plus_minus."""
    if not rows:
        return 0
    # nba_api returns "Game_ID" not "GAME_ID"; use _get so we don't insert empty game_id (which would collapse to 1 row per player)
    by_game = {str(_get(r, "GAME_ID", "Game_ID") or ""): r for r in rows}
    rows = list(by_game.values())
    values = []
    for r in rows:
        team_abbr, opponent_abbr, home_away = parse_matchup(r.get("MATCHUP"))
        # Store normalized season (e.g. "2025-26"), not raw SEASON_ID ("22025")
        season_val = r.get("SEASON") or _normalize_season_id(r.get("SEASON_ID"))
        values.append((
            player_id,
            str(_get(r, "GAME_ID", "Game_ID") or ""),
            _get(r, "GAME_DATE", "Game_Date") or r.get("GAME_DATE"),
            season_val or "",
            r.get("MATCHUP") or "",
            home_away,
            team_abbr,
            opponent_abbr,
            (r.get("WL") or "")[:1],
            _int(r.get("MIN")),
            _int(r.get("PTS"), 0),
            _int(r.get("REB"), 0),
            _int(r.get("AST"), 0),
            _int(r.get("STL"), 0),
            _int(r.get("BLK"), 0),
            _int(r.get("TOV"), 0),
            _int(r.get("FGM"), 0),
            _int(r.get("FGA"), 0),
            _int(r.get("FG3M"), 0),
            _int(r.get("FG3A"), 0),
            _int(r.get("FTM"), 0),
            _int(r.get("FTA"), 0),
            _int(r.get("OREB"), 0),
            _int(r.get("DREB"), 0),
            _int(r.get("PF"), 0),
            _int(r.get("PLUS_MINUS")),
        ))
    with conn.cursor() as cur:
        execute_values(
            cur,
            """
            INSERT INTO player_game_logs (
                player_id, nba_game_id, game_date, season, matchup, home_away,
                team_abbr, opponent_abbr, wl, min_played, pts, reb, ast, stl, blk, tov,
                fgm, fga, fg3m, fg3a, ftm, fta, oreb, dreb, pf, plus_minus
            ) VALUES %s
            ON CONFLICT (player_id, nba_game_id) DO UPDATE SET
                game_date = EXCLUDED.game_date,
                season = EXCLUDED.season,
                matchup = EXCLUDED.matchup,
                home_away = EXCLUDED.home_away,
                team_abbr = EXCLUDED.team_abbr,
                opponent_abbr = EXCLUDED.opponent_abbr,
                wl = EXCLUDED.wl,
                min_played = EXCLUDED.min_played,
                pts = EXCLUDED.pts,
                reb = EXCLUDED.reb,
                ast = EXCLUDED.ast,
                stl = EXCLUDED.stl,
                blk = EXCLUDED.blk,
                tov = EXCLUDED.tov,
                fgm = EXCLUDED.fgm,
                fga = EXCLUDED.fga,
                fg3m = EXCLUDED.fg3m,
                fg3a = EXCLUDED.fg3a,
                ftm = EXCLUDED.ftm,
                fta = EXCLUDED.fta,
                oreb = EXCLUDED.oreb,
                dreb = EXCLUDED.dreb,
                pf = EXCLUDED.pf,
                plus_minus = EXCLUDED.plus_minus,
                updated_at = NOW()
            """,
            values,
            template="(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        )
    return len(values)


def upsert_player_season_stats(conn, player_id: int, season: str, rows: list[dict]) -> None:
    """Compute season aggregates from game log rows and upsert into player_season_stats."""
    if not rows:
        return
    n = len(rows)
    pts_sum = sum(_int(r.get("PTS"), 0) for r in rows)
    reb_sum = sum(_int(r.get("REB"), 0) for r in rows)
    ast_sum = sum(_int(r.get("AST"), 0) for r in rows)
    stl_sum = sum(_int(r.get("STL"), 0) for r in rows)
    blk_sum = sum(_int(r.get("BLK"), 0) for r in rows)
    tov_sum = sum(_int(r.get("TOV"), 0) for r in rows)
    fgm_sum = sum(_int(r.get("FGM"), 0) for r in rows)
    fga_sum = sum(_int(r.get("FGA"), 0) for r in rows)
    fg3m_sum = sum(_int(r.get("FG3M"), 0) for r in rows)
    fg3a_sum = sum(_int(r.get("FG3A"), 0) for r in rows)
    ftm_sum = sum(_int(r.get("FTM"), 0) for r in rows)
    fta_sum = sum(_int(r.get("FTA"), 0) for r in rows)
    pts_avg = round(pts_sum / n, 2) if n else 0
    reb_avg = round(reb_sum / n, 2) if n else 0
    ast_avg = round(ast_sum / n, 2) if n else 0
    stl_avg = round(stl_sum / n, 2) if n else 0
    blk_avg = round(blk_sum / n, 2) if n else 0
    tov_avg = round(tov_sum / n, 2) if n else 0
    fg_pct = round(fgm_sum / fga_sum, 3) if fga_sum else None
    fg3_pct = round(fg3m_sum / fg3a_sum, 3) if fg3a_sum else None
    ft_pct = round(ftm_sum / fta_sum, 3) if fta_sum else None
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO player_season_stats (player_id, season, games_played, pts_avg, reb_avg, ast_avg, stl_avg, blk_avg, tov_avg, fg_pct, fg3_pct, ft_pct)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (player_id, season) DO UPDATE SET
                games_played = EXCLUDED.games_played,
                pts_avg = EXCLUDED.pts_avg,
                reb_avg = EXCLUDED.reb_avg,
                ast_avg = EXCLUDED.ast_avg,
                stl_avg = EXCLUDED.stl_avg,
                blk_avg = EXCLUDED.blk_avg,
                tov_avg = EXCLUDED.tov_avg,
                fg_pct = EXCLUDED.fg_pct,
                fg3_pct = EXCLUDED.fg3_pct,
                ft_pct = EXCLUDED.ft_pct,
                updated_at = NOW()
            """,
            (player_id, season, n, pts_avg, reb_avg, ast_avg, stl_avg, blk_avg, tov_avg, fg_pct, fg3_pct, ft_pct),
        )
    logger.info("Upserted player_season_stats for player_id=%s season=%s (gp=%s)", player_id, season, n)


def _int(v, default=None):
    if v is None or v == "":
        return default
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return default


def _get(d, *keys, default=None):
    """Get first present key from dict (handles API returning Game_ID vs GAME_ID, etc.)."""
    for k in keys:
        if k in d and d[k] is not None and d[k] != "":
            return d[k]
    return default


def _run_with_timeout(func, timeout_seconds: float, default=None):
    """Run func() in a thread; return result or default on timeout (avoids hanging on nba_api)."""
    with concurrent.futures.ThreadPoolExecutor(max_workers=1) as ex:
        fut = ex.submit(func)
        try:
            return fut.result(timeout=timeout_seconds)
        except concurrent.futures.TimeoutError:
            logger.warning("Request timed out after %s s", timeout_seconds)
            return default


def fetch_game_log(nba_player_id: int, season: str, delay: float = REQUEST_DELAY, retry_on_short: bool = True) -> list[dict]:
    """Fetch game log for one player/season from nba_api. Returns list of row dicts.
    Uses the result set with the most rows that has GAME_ID (full game log); retries once if 0–1 row.
    """
    time.sleep(delay)
    try:
        log = playergamelog.PlayerGameLog(player_id=nba_player_id, season=season, season_type_all_star="Regular Season")
        # get_dict() returns {"resultSets": [{"name": "...", "headers": [...], "rowSet": [[...], ...]}, ...]}
        # Pick the result set that looks like the game log (has GAME_ID and most rows)
        raw = log.get_dict()
    except Exception as e:
        logger.warning("nba_api playergamelog failed for player_id=%s season=%s: %s", nba_player_id, season, e)
        return []

    result_sets = raw.get("resultSets") or raw.get("result_sets") or []
    best_rows = []
    best_headers = None
    for rs in result_sets:
        if not isinstance(rs, dict):
            continue
        headers = rs.get("headers") or []
        row_set = rs.get("rowSet") or rs.get("row_set") or []
        # nba_api / NBA.com return "Game_ID" (capital G), not "GAME_ID"
        if not headers or ("GAME_ID" not in headers and "Game_ID" not in headers):
            continue
        if len(row_set) > len(best_rows):
            best_rows = row_set
            best_headers = headers

    if not best_headers or not best_rows:
        # Fallback: try get_data_frames()[0] as before
        try:
            log2 = playergamelog.PlayerGameLog(player_id=nba_player_id, season=season, season_type_all_star="Regular Season")
            df = log2.get_data_frames()[0]
        except Exception:
            df = None
        if df is not None and not df.empty and ("GAME_ID" in df.columns or "Game_ID" in df.columns):
            best_headers = list(df.columns)
            best_rows = df.values.tolist()
        else:
            logger.warning("No game log result set for player_id=%s season=%s (got %s rows)", nba_player_id, season, len(best_rows))
            if retry_on_short and len(best_rows) <= 1:
                logger.info("Retrying in 2s for player_id=%s season=%s...", nba_player_id, season)
                time.sleep(2)
                return fetch_game_log(nba_player_id, season, delay=0, retry_on_short=False)
            return []

    # Build list of dicts: [{"GAME_ID": ..., "GAME_DATE": ..., ...}, ...]
    rows = [dict(zip(best_headers, r)) for r in best_rows]

    # Normalize season: NBA returns e.g. "22025" for 2025-26
    if rows and "SEASON_ID" in rows[0]:
        raw_sid = str(rows[0].get("SEASON_ID", ""))
        if len(raw_sid) >= 5 and raw_sid.isdigit():
            start_year = int(raw_sid[1:5])
            season_used = f"{start_year}-{str((start_year + 1) % 100).zfill(2)}"
        else:
            season_used = season
    else:
        season_used = season

    for r in rows:
        r["SEASON"] = season_used

    logger.info("Fetched %s game log rows for player_id=%s season=%s", len(rows), nba_player_id, season_used)
    # If we got 0 or 1 row, retry once (NBA.com sometimes returns partial data when rate-limited)
    if retry_on_short and len(rows) <= 1:
        logger.info("Only %s row(s) returned; retrying once in 2s...", len(rows))
        time.sleep(2)
        return fetch_game_log(nba_player_id, season, delay=0, retry_on_short=False)

    return rows


def search_players(name_query: str) -> list[tuple[int, str]]:
    """Return list of (nba_player_id, full_name) matching name (case-insensitive substring)."""
    all_players = nba_players_static.get_players()
    q = (name_query or "").strip().lower()
    if not q:
        return []
    return [(p["id"], p["full_name"]) for p in all_players if q in p["full_name"].lower()]


def main():
    parser = argparse.ArgumentParser(description="Ingest NBA players and game logs into PostgreSQL")
    parser.add_argument("--player", type=str, help="Search and ingest by player name (e.g. 'LeBron')")
    parser.add_argument("--player-id", type=int, help="Ingest by NBA player ID (e.g. 2544 for LeBron)")
    parser.add_argument("--season", type=str, default=None, help="Season, e.g. 2024-25 (default: current season)")
    parser.add_argument("--delay", type=float, default=REQUEST_DELAY, help="Seconds between API calls")
    parser.add_argument("--dry-run", action="store_true", help="Do not write to DB")
    args = parser.parse_args()
    if args.season is None:
        args.season = get_current_season()

    to_ingest: list[int] = []
    if args.player_id:
        to_ingest.append(args.player_id)
    elif args.player:
        matches = search_players(args.player)
        if not matches:
            logger.error("No players found for query: %s", args.player)
            raise SystemExit(1)
        if len(matches) > 1:
            logger.info("Multiple matches: %s", [m[1] for m in matches])
        to_ingest.append(matches[0][0])
        logger.info("Ingesting: %s (id=%s)", matches[0][1], matches[0][0])
    else:
        # Default: resolve each name in DEFAULT_PLAYER_NAMES to NBA ID (dedupe by ID)
        seen_ids = set()
        for name in DEFAULT_PLAYER_NAMES:
            name = (name or "").strip()
            if not name:
                continue
            matches = search_players(name)
            if not matches:
                logger.warning("No match for name: %s (skipping)", name)
                continue
            nba_id = matches[0][0]
            full_name = matches[0][1]
            if nba_id in seen_ids:
                continue
            seen_ids.add(nba_id)
            to_ingest.append(nba_id)
            logger.info("Queued: %s (id=%s)", full_name, nba_id)
        logger.info("No --player or --player-id: ingesting %s players for %s", len(to_ingest), args.season)

    if args.dry_run:
        logger.info("Dry run: would ingest nba_player_ids=%s season=%s", to_ingest, args.season)
        return

    with db_connection() as conn:
        for nba_id in to_ingest:
            # Resolve name for display and for DB (with timeout so one hung request doesn't stall the run)
            time.sleep(args.delay)

            def _fetch_player_info():
                try:
                    info = commonplayerinfo.CommonPlayerInfo(player_id=nba_id)
                    return info.get_data_frames()[0]
                except Exception as e:
                    logger.warning("commonplayerinfo failed for nba_id=%s: %s", nba_id, e)
                    return None

            info_df = _run_with_timeout(_fetch_player_info, timeout_seconds=60, default=None)
            if info_df is not None and not info_df.empty:
                full_name = info_df["DISPLAY_FIRST_LAST"].iloc[0]
                first_name = info_df["FIRST_NAME"].iloc[0]
                last_name = info_df["LAST_NAME"].iloc[0]
            else:
                static = [p for p in nba_players_static.get_players() if p["id"] == nba_id]
                full_name = static[0]["full_name"] if static else f"Player_{nba_id}"
                first_name = last_name = None

            player_id = ensure_player(conn, nba_id, full_name, first_name, last_name)
            logger.info("Player id=%s nba_id=%s name=%s", player_id, nba_id, full_name)

            rows = _run_with_timeout(
                lambda: fetch_game_log(nba_id, args.season, args.delay),
                timeout_seconds=90,
                default=[],
            )
            if rows:
                n = upsert_game_logs(conn, player_id, rows)
                logger.info("Upserted %s game log rows for player_id=%s season=%s", n, player_id, args.season)
                season_used = rows[0].get("SEASON") or args.season
                upsert_player_season_stats(conn, player_id, season_used, rows)
            else:
                logger.warning("No game log rows for player_id=%s season=%s", player_id, args.season)

            # Commit after each player so we don't sit "idle in transaction" for the whole run.
            # Avoids Railway/free-tier closing long-lived idle transactions.
            conn.commit()

    logger.info("Ingestion done.")


if __name__ == "__main__":
    main()
