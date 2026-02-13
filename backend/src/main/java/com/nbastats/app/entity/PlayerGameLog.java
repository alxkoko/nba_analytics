package com.nbastats.app.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "player_game_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "player_id", "nba_game_id" })
})
public class PlayerGameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "nba_game_id", nullable = false)
    private String nbaGameId;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "season", nullable = false, length = 9)
    private String season;

    @Column(name = "matchup", length = 32)
    private String matchup;

    @Column(name = "home_away", length = 1)
    private String homeAway;

    @Column(name = "team_abbr", length = 8)
    private String teamAbbr;

    @Column(name = "opponent_abbr", length = 8)
    private String opponentAbbr;

    @Column(name = "wl", length = 1)
    private String wl;

    @Column(name = "min_played")
    private Integer minPlayed;

    @Column(name = "pts", nullable = false)
    private Integer pts = 0;

    @Column(name = "reb", nullable = false)
    private Integer reb = 0;

    @Column(name = "ast", nullable = false)
    private Integer ast = 0;

    @Column(name = "stl", nullable = false)
    private Integer stl = 0;

    @Column(name = "blk", nullable = false)
    private Integer blk = 0;

    @Column(name = "tov", nullable = false)
    private Integer tov = 0;

    @Column(name = "fgm", nullable = false)
    private Integer fgm = 0;

    @Column(name = "fga", nullable = false)
    private Integer fga = 0;

    @Column(name = "fg3m", nullable = false)
    private Integer fg3m = 0;

    @Column(name = "fg3a", nullable = false)
    private Integer fg3a = 0;

    @Column(name = "ftm", nullable = false)
    private Integer ftm = 0;

    @Column(name = "fta", nullable = false)
    private Integer fta = 0;

    @Column(name = "oreb", nullable = false)
    private Integer oreb = 0;

    @Column(name = "dreb", nullable = false)
    private Integer dreb = 0;

    @Column(name = "pf", nullable = false)
    private Integer pf = 0;

    @Column(name = "plus_minus")
    private Integer plusMinus;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public String getNbaGameId() { return nbaGameId; }
    public void setNbaGameId(String nbaGameId) { this.nbaGameId = nbaGameId; }

    public LocalDate getGameDate() { return gameDate; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getMatchup() { return matchup; }
    public void setMatchup(String matchup) { this.matchup = matchup; }

    public String getHomeAway() { return homeAway; }
    public void setHomeAway(String homeAway) { this.homeAway = homeAway; }

    public String getTeamAbbr() { return teamAbbr; }
    public void setTeamAbbr(String teamAbbr) { this.teamAbbr = teamAbbr; }

    public String getOpponentAbbr() { return opponentAbbr; }
    public void setOpponentAbbr(String opponentAbbr) { this.opponentAbbr = opponentAbbr; }

    public String getWl() { return wl; }
    public void setWl(String wl) { this.wl = wl; }

    public Integer getMinPlayed() { return minPlayed; }
    public void setMinPlayed(Integer minPlayed) { this.minPlayed = minPlayed; }

    public Integer getPts() { return pts; }
    public void setPts(Integer pts) { this.pts = pts; }

    public Integer getReb() { return reb; }
    public void setReb(Integer reb) { this.reb = reb; }

    public Integer getAst() { return ast; }
    public void setAst(Integer ast) { this.ast = ast; }

    public Integer getStl() { return stl; }
    public void setStl(Integer stl) { this.stl = stl; }

    public Integer getBlk() { return blk; }
    public void setBlk(Integer blk) { this.blk = blk; }

    public Integer getTov() { return tov; }
    public void setTov(Integer tov) { this.tov = tov; }

    public Integer getFgm() { return fgm; }
    public void setFgm(Integer fgm) { this.fgm = fgm; }

    public Integer getFga() { return fga; }
    public void setFga(Integer fga) { this.fga = fga; }

    public Integer getFg3m() { return fg3m; }
    public void setFg3m(Integer fg3m) { this.fg3m = fg3m; }

    public Integer getFg3a() { return fg3a; }
    public void setFg3a(Integer fg3a) { this.fg3a = fg3a; }

    public Integer getFtm() { return ftm; }
    public void setFtm(Integer ftm) { this.ftm = ftm; }

    public Integer getFta() { return fta; }
    public void setFta(Integer fta) { this.fta = fta; }

    public Integer getOreb() { return oreb; }
    public void setOreb(Integer oreb) { this.oreb = oreb; }

    public Integer getDreb() { return dreb; }
    public void setDreb(Integer dreb) { this.dreb = dreb; }

    public Integer getPf() { return pf; }
    public void setPf(Integer pf) { this.pf = pf; }

    public Integer getPlusMinus() { return plusMinus; }
    public void setPlusMinus(Integer plusMinus) { this.plusMinus = plusMinus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
