package com.nbastats.app.dto;

public record OverUnderDto(
    String stat,
    double threshold,
    int totalGames,
    int gamesOver,
    int gamesUnder,
    double probabilityOver,
    double probabilityUnder,
    Integer lastN
) {}
