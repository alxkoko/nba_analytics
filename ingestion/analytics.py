"""
Over/under probability logic for NBA player stats.
Uses historical game-by-game data: P(stat >= X) = count(games where stat >= X) / total_games.
Designed so the same formulas can be implemented in Spring Boot (SQL or in-memory).
"""

from __future__ import annotations


def over_under_probability(
    values: list[int | float],
    threshold: int | float,
    *,
    last_n: int | None = None,
) -> dict:
    """
    Compute how often a stat was at or above a threshold.

    Args:
        values: List of stat values per game (e.g. points), in chronological order (oldest first).
        threshold: Over/under line (e.g. 25.5 for "over 25.5 points").
        last_n: If set, use only the last N games. None = use all.

    Returns:
        {
            "total_games": int,
            "games_over": int,   # count of games where value >= threshold
            "games_under": int,
            "probability_over": float in [0, 1],
            "probability_under": float in [0, 1],
        }
    """
    if last_n is not None and last_n > 0:
        values = values[-last_n:]
    if not values:
        return {
            "total_games": 0,
            "games_over": 0,
            "games_under": 0,
            "probability_over": 0.0,
            "probability_under": 0.0,
        }
    total = len(values)
    over = sum(1 for v in values if v is not None and v >= threshold)
    under = total - over
    return {
        "total_games": total,
        "games_over": over,
        "games_under": under,
        "probability_over": over / total if total else 0.0,
        "probability_under": under / total if total else 0.0,
    }


def last_n_averages(
    values: list[int | float],
    last_n: int,
) -> dict:
    """
    Stats for the last N games: count, sum, average, min, max.

    Args:
        values: Stat per game (chronological order).
        last_n: Number of most recent games.

    Returns:
        {"count", "sum", "avg", "min", "max"} for the last_n values.
    """
    slice_ = [v for v in values[-last_n:] if v is not None]
    if not slice_:
        return {"count": 0, "sum": 0, "avg": 0.0, "min": None, "max": None}
    return {
        "count": len(slice_),
        "sum": sum(slice_),
        "avg": sum(slice_) / len(slice_),
        "min": min(slice_),
        "max": max(slice_),
    }


def over_under_for_stat(
    game_rows: list[dict],
    stat_key: str,
    threshold: int | float,
    *,
    last_n: int | None = None,
) -> dict:
    """
    Compute over/under probability for a stat from a list of game rows.

    Args:
        game_rows: List of dicts with at least {stat_key: number}, in chronological order.
        stat_key: Key for the stat (e.g. "pts", "reb", "ast").
        threshold: Over/under line.
        last_n: If set, use only the last N games.

    Returns:
        Same shape as over_under_probability() plus "stat" and "threshold".
    """
    values = [r.get(stat_key) for r in game_rows]
    out = over_under_probability(values, threshold, last_n=last_n)
    out["stat"] = stat_key
    out["threshold"] = threshold
    return out


# --- Optional: DB-backed helpers (require psycopg2 and schema) ---

def get_over_under_from_db(conn, player_id: int, season: str, stat: str, threshold: float, last_n: int | None = None) -> dict:
    """
    Load game log from DB and compute over/under. Use when you already have a DB connection.

    Args:
        conn: psycopg2 connection.
        player_id: Our players.id (not NBA id).
        season: e.g. "2024-25".
        stat: Column name: pts, reb, ast, etc.
        threshold: Over/under line.
        last_n: If set, only last N games (by game_date desc).

    Returns:
        Same as over_under_for_stat().
    """
    import psycopg2.extras
    allowed = {"pts", "reb", "ast", "stl", "blk", "tov", "min_played", "fgm", "fga", "ftm", "fta"}
    if stat not in allowed:
        stat = "pts"
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        if last_n:
            cur.execute(
                f"""
                SELECT game_date, {stat} AS val
                FROM player_game_logs
                WHERE player_id = %s AND season = %s
                ORDER BY game_date DESC
                LIMIT %s
                """,
                (player_id, season, last_n),
            )
            rows = list(reversed(cur.fetchall()))  # chronological for probability
        else:
            cur.execute(
                f"""
                SELECT game_date, {stat} AS val
                FROM player_game_logs
                WHERE player_id = %s AND season = %s
                ORDER BY game_date ASC
                """,
                (player_id, season),
            )
            rows = cur.fetchall()
    values = [r["val"] for r in rows]
    out = over_under_probability(values, threshold, last_n=None)
    out["stat"] = stat
    out["threshold"] = threshold
    return out
