package com.nbastats.app.service;

import com.nbastats.app.dto.GameLogDto;
import com.nbastats.app.dto.OverUnderDto;
import com.nbastats.app.dto.SeasonStatsDto;
import com.nbastats.app.entity.Player;
import com.nbastats.app.entity.PlayerGameLog;
import com.nbastats.app.repository.PlayerGameLogRepository;
import com.nbastats.app.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerGameLogRepository gameLogRepository;

    private static final List<String> ALLOWED_STATS = List.of(
        "pts", "reb", "ast", "fg3m", "stl", "blk", "tov", "min_played",
        "pts_reb", "pts_ast", "reb_ast", "pts_reb_ast"
    );

    public PlayerService(PlayerRepository playerRepository, PlayerGameLogRepository gameLogRepository) {
        this.playerRepository = playerRepository;
        this.gameLogRepository = gameLogRepository;
    }

    public List<Player> searchByName(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return playerRepository.searchByName(q.trim());
    }

    public Player getById(Long id) {
        return playerRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GameLogDto> getGameLog(Long playerId, String season) {
        List<PlayerGameLog> logs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        return logs.stream().map(GameLogDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeasonStatsDto getSeasonStats(Long playerId, String season) {
        List<PlayerGameLog> logs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        if (logs.isEmpty()) {
            return new SeasonStatsDto(season, 0, 0, 0, 0, 0, 0, 0);
        }
        int n = logs.size();
        double pts = logs.stream().mapToInt(PlayerGameLog::getPts).sum() / (double) n;
        double reb = logs.stream().mapToInt(PlayerGameLog::getReb).sum() / (double) n;
        double ast = logs.stream().mapToInt(PlayerGameLog::getAst).sum() / (double) n;
        double stl = logs.stream().mapToInt(PlayerGameLog::getStl).sum() / (double) n;
        double blk = logs.stream().mapToInt(PlayerGameLog::getBlk).sum() / (double) n;
        double tov = logs.stream().mapToInt(PlayerGameLog::getTov).sum() / (double) n;
        return new SeasonStatsDto(season, n, pts, reb, ast, stl, blk, tov);
    }

    @Transactional(readOnly = true)
    public OverUnderDto getOverUnder(Long playerId, String season, String stat, double threshold, Integer lastN) {
        String safeStat = ALLOWED_STATS.contains(stat) ? stat : "pts";
        List<PlayerGameLog> logs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        if (lastN != null && lastN > 0) {
            logs = logs.stream().limit(lastN).toList();
        }
        if (logs.isEmpty()) {
            return new OverUnderDto(safeStat, threshold, 0, 0, 0, 0.0, 0.0, lastN);
        }
        List<Number> values = logs.stream()
            .map(g -> getStatValue(g, safeStat))
            .toList();
        int total = values.size();
        long over = values.stream()
            .filter(v -> v != null && v.doubleValue() >= threshold)
            .count();
        int under = total - (int) over;
        double probOver = total > 0 ? (double) over / total : 0.0;
        double probUnder = total > 0 ? (double) under / total : 0.0;
        return new OverUnderDto(safeStat, threshold, total, (int) over, under, probOver, probUnder, lastN);
    }

    private Number getStatValue(PlayerGameLog g, String stat) {
        return switch (stat) {
            case "pts" -> g.getPts();
            case "reb" -> g.getReb();
            case "ast" -> g.getAst();
            case "fg3m" -> g.getFg3m();
            case "stl" -> g.getStl();
            case "blk" -> g.getBlk();
            case "tov" -> g.getTov();
            case "min_played" -> g.getMinPlayed();
            case "pts_reb" -> g.getPts() + g.getReb();
            case "pts_ast" -> g.getPts() + g.getAst();
            case "reb_ast" -> g.getReb() + g.getAst();
            case "pts_reb_ast" -> g.getPts() + g.getReb() + g.getAst();
            default -> g.getPts();
        };
    }
}
