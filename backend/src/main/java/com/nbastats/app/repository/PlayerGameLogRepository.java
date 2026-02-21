package com.nbastats.app.repository;

import com.nbastats.app.entity.PlayerGameLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerGameLogRepository extends JpaRepository<PlayerGameLog, Long> {

    List<PlayerGameLog> findByPlayer_IdAndSeasonOrderByGameDateDesc(Long playerId, String season);

    /** Latest team_abbr per player for the given season (Postgres DISTINCT ON). Returns [playerId, teamAbbr]. */
    @Query(value = "SELECT player_id, team_abbr FROM (SELECT player_id, team_abbr, ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY game_date DESC) AS rn FROM player_game_logs WHERE player_id IN :playerIds AND season = :season) sub WHERE rn = 1", nativeQuery = true)
    List<Object[]> findLatestTeamAbbrByPlayerIdsAndSeason(@Param("playerIds") List<Long> playerIds, @Param("season") String season);
}
