package com.nbastats.app.dto;

import com.nbastats.app.entity.PlayerGameLog;

import java.time.LocalDate;

public record GameLogDto(
    Long id,
    String nbaGameId,
    LocalDate gameDate,
    String season,
    String matchup,
    String homeAway,
    String teamAbbr,
    String opponentAbbr,
    String wl,
    Integer minPlayed,
    Integer pts,
    Integer reb,
    Integer ast,
    Integer stl,
    Integer blk,
    Integer tov,
    Integer fg3m
) {
    public static GameLogDto from(PlayerGameLog log) {
        return new GameLogDto(
            log.getId(),
            log.getNbaGameId(),
            log.getGameDate(),
            log.getSeason(),
            log.getMatchup(),
            log.getHomeAway(),
            log.getTeamAbbr(),
            log.getOpponentAbbr(),
            log.getWl(),
            log.getMinPlayed(),
            log.getPts(),
            log.getReb(),
            log.getAst(),
            log.getStl(),
            log.getBlk(),
            log.getTov(),
            log.getFg3m()
        );
    }
}
