package com.nbastats.app.repository;

import com.nbastats.app.entity.PlayerGameLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlayerGameLogRepository extends JpaRepository<PlayerGameLog, Long> {

    List<PlayerGameLog> findByPlayer_IdAndSeasonOrderByGameDateDesc(Long playerId, String season);

    /** Latest team_abbr per player for the given season (Postgres DISTINCT ON). Returns [playerId, teamAbbr]. */
    @Query(value = "SELECT player_id, team_abbr FROM (SELECT player_id, team_abbr, ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY game_date DESC) AS rn FROM player_game_logs WHERE player_id IN :playerIds AND season = :season) sub WHERE rn = 1", nativeQuery = true)
    List<Object[]> findLatestTeamAbbrByPlayerIdsAndSeason(@Param("playerIds") List<Long> playerIds, @Param("season") String season);

    /** Player ids that have at least 10 games in the season on or before the given date (for excluding low-sample picks). */
    @Query("SELECT g.player.id FROM PlayerGameLog g WHERE g.player.id IN :playerIds AND g.season = :season AND g.gameDate <= :onOrBefore GROUP BY g.player.id HAVING COUNT(g) >= 10")
    List<Long> findPlayerIdsWithAtLeast10GamesBefore(@Param("playerIds") List<Long> playerIds, @Param("season") String season, @Param("onOrBefore") LocalDate onOrBefore);
}
