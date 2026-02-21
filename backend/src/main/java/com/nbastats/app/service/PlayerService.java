package com.nbastats.app.service;

import com.nbastats.app.dto.GameLogDto;
import com.nbastats.app.dto.OverUnderDto;
import com.nbastats.app.dto.PropPickSuggestionDto;
import com.nbastats.app.dto.SeasonStatsDto;
import com.nbastats.app.entity.Player;
import com.nbastats.app.entity.PlayerGameLog;
import com.nbastats.app.repository.PlayerGameLogRepository;
import com.nbastats.app.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private static final java.util.Map<String, String> STAT_LABELS = java.util.Map.ofEntries(
        java.util.Map.entry("pts", "points"),
        java.util.Map.entry("reb", "rebounds"),
        java.util.Map.entry("ast", "assists"),
        java.util.Map.entry("fg3m", "3's"),
        java.util.Map.entry("stl", "steals"),
        java.util.Map.entry("blk", "blocks"),
        java.util.Map.entry("tov", "turnovers"),
        java.util.Map.entry("min_played", "minutes"),
        java.util.Map.entry("pts_reb", "Pts+Reb"),
        java.util.Map.entry("pts_ast", "Pts+Ast"),
        java.util.Map.entry("reb_ast", "Reb+Ast"),
        java.util.Map.entry("pts_reb_ast", "Pts+Reb+Ast")
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

    @Transactional(readOnly = true)
    public List<PropPickSuggestionDto> getPropPickSuggestions(Long playerId, String season,
                                                              Double ptsRebAstLine, Double ptsAstLine, Double rebAstLine) {
        List<PlayerGameLog> logs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        if (logs.size() < 10) {
            return List.of();
        }
        logs = logs.stream().limit(10).toList();
        List<PropPickSuggestionDto> out = new ArrayList<>();
        if (ptsRebAstLine != null) {
            out.add(buildPropSuggestion(logs, "pts_reb_ast", "Pts+Reb+Ast", ptsRebAstLine));
        }
        if (ptsAstLine != null) {
            out.add(buildPropSuggestion(logs, "pts_ast", "Pts+Ast", ptsAstLine));
        }
        if (rebAstLine != null) {
            out.add(buildPropSuggestion(logs, "reb_ast", "Reb+Ast", rebAstLine));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PropPickSuggestionDto getSinglePropSuggestion(Long playerId, String season, String statKey, double lineValue) {
        String safeStat = ALLOWED_STATS.contains(statKey) ? statKey : "pts";
        String label = STAT_LABELS.getOrDefault(safeStat, safeStat);
        List<PlayerGameLog> logs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        if (logs.size() < 10) {
            return null;
        }
        logs = logs.stream().limit(10).toList();
        return buildPropSuggestion(logs, safeStat, label, lineValue);
    }

    public static String getStatLabel(String statKey) {
        return STAT_LABELS.getOrDefault(statKey != null ? statKey : "pts", "points");
    }

    public static List<String> getAllowedStats() {
        return ALLOWED_STATS;
    }

    private PropPickSuggestionDto buildPropSuggestion(List<PlayerGameLog> logs, String statKey, String propLabel, double line) {
        List<Double> values = logs.stream()
            .map(g -> getStatValue(g, statKey).doubleValue())
            .toList();
        int n = values.size();
        int n5 = Math.min(5, n);
        double last10Avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double last5Avg = values.stream().limit(5).mapToDouble(Double::doubleValue).average().orElse(0);
        long hit10 = values.stream().filter(v -> v >= line).count();
        long over5 = values.stream().limit(5).filter(v -> v >= line).count();
        double stdDev = stdDev(values);
        boolean highVariance = n > 1 && stdDev > 0.3 * last10Avg;
        double maxLast5 = values.stream().limit(5).mapToDouble(Double::doubleValue).max().orElse(0);
        boolean oneBigGame = n5 >= 3 && maxLast5 > last5Avg + 2 * stdDev(values.stream().limit(5).toList());
        String varianceNote = oneBigGame ? "One big game in last 5 — tread carefully" : (highVariance ? "High variance" : "Consistent");
        double projected = 0.6 * last5Avg + 0.4 * last10Avg;
        String suggestion;
        String confidence;
        if (projected >= line + 1.5 && (int) over5 >= 3 && !oneBigGame) {
            suggestion = "Over";
            confidence = highVariance ? "Medium" : "High";
        } else if (projected >= line && (int) hit10 >= 5) {
            suggestion = "Over";
            confidence = oneBigGame ? "Low" : (highVariance ? "Medium" : "High");
        } else if (projected <= line - 1.5 && (int) over5 <= 2) {
            suggestion = "Under";
            confidence = highVariance ? "Medium" : "High";
        } else if (projected <= line && (int) hit10 <= 5) {
            suggestion = "Under";
            confidence = oneBigGame ? "Low" : (highVariance ? "Medium" : "High");
        } else {
            suggestion = projected >= line ? "Over" : "Under";
            confidence = "Low";
        }
        return new PropPickSuggestionDto(propLabel, statKey, line, suggestion, confidence,
            Math.round(last10Avg * 10) / 10.0, Math.round(last5Avg * 10) / 10.0, "",
            (int) hit10, (int) over5, varianceNote);
    }

    private double stdDev(List<Double> values) {
        if (values.size() < 2) return 0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / values.size();
        return Math.sqrt(variance);
    }

    private Number getStatValue(PlayerGameLog g, String stat) {
        Integer minPlayed = g.getMinPlayed();
        int min = (minPlayed != null) ? minPlayed : 0;
        return switch (stat) {
            case "pts" -> g.getPts();
            case "reb" -> g.getReb();
            case "ast" -> g.getAst();
            case "fg3m" -> g.getFg3m() != null ? g.getFg3m() : 0;
            case "stl" -> g.getStl();
            case "blk" -> g.getBlk();
            case "tov" -> g.getTov();
            case "min_played" -> min;
            case "pts_reb" -> g.getPts() + g.getReb();
            case "pts_ast" -> g.getPts() + g.getAst();
            case "reb_ast" -> g.getReb() + g.getAst();
            case "pts_reb_ast" -> g.getPts() + g.getReb() + g.getAst();
            default -> g.getPts();
        };
    }
}
