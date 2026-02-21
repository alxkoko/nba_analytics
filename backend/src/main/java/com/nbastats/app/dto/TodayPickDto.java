package com.nbastats.app.dto;

/** One pick shown on homepage (from daily_prop_lines). */
public record TodayPickDto(
    long id,
    String playerName,
    long playerId,
    String statLabel,   // e.g. "3's", "points"
    String statKey,
    double line,
    String suggestion,  // Over, Under
    String confidence,
    String reason,
    String teamAbbr,    // optional, for diversity; may be null
    int hitRateLast10,  // 0–10; -1 if not stored (old data)
    int overLast5       // 0–5; -1 if not stored (old data)
) {}
