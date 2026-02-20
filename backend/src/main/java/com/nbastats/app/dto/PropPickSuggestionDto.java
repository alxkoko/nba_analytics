package com.nbastats.app.dto;

/** Result of prop-pick suggestion logic (last 10/5 games, trend, variance). */
public record PropPickSuggestionDto(
    String propLabel,
    String statKey,
    double line,
    String suggestion,   // Over, Under
    String confidence,  // High, Medium, Low
    double last10Avg,
    double last5Avg,
    String trend,       // Up, Down
    int hitRateLast10,
    int overLast5,
    String varianceNote
) {}
