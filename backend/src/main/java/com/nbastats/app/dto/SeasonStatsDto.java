package com.nbastats.app.dto;

public record SeasonStatsDto(
    String season,
    int gamesPlayed,
    double ptsAvg,
    double rebAvg,
    double astAvg,
    double stlAvg,
    double blkAvg,
    double tovAvg
) {}
