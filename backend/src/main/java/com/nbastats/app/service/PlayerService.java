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
        List<PlayerGameLog> allLogs = gameLogRepository.findByPlayer_IdAndSeasonOrderByGameDateDesc(playerId, season);
        if (allLogs.size() < 10) {
            return null;
        }
        List<PlayerGameLog> logs = allLogs.stream().limit(10).toList();

        int seasonHits = (int) allLogs.stream()
            .filter(g -> getStatValue(g, safeStat).doubleValue() >= lineValue)
            .count();
        int seasonTotal = allLogs.size();

        Double seasonAvg3pm = null;
        Double season3pPct = null;
        if ("fg3m".equals(safeStat)) {
            int seasonFg3m = allLogs.stream().mapToInt(g -> g.getFg3m() != null ? g.getFg3m() : 0).sum();
            int seasonFg3a = allLogs.stream().mapToInt(g -> g.getFg3a() != null ? g.getFg3a() : 0).sum();
            seasonAvg3pm = (double) seasonFg3m / allLogs.size();
            season3pPct = seasonFg3a > 0 ? (double) seasonFg3m / seasonFg3a : null;
        }
        return buildPropSuggestion(logs, safeStat, label, lineValue, seasonAvg3pm, season3pPct, seasonHits, seasonTotal);
    }

    public static String getStatLabel(String statKey) {
        return STAT_LABELS.getOrDefault(statKey != null ? statKey : "pts", "points");
    }

    public static List<String> getAllowedStats() {
        return ALLOWED_STATS;
    }

    private PropPickSuggestionDto buildPropSuggestion(List<PlayerGameLog> logs, String statKey, String propLabel, double line) {
        return buildPropSuggestion(logs, statKey, propLabel, line, null, null, 0, 0);
    }

    private PropPickSuggestionDto buildPropSuggestion(List<PlayerGameLog> logs, String statKey, String propLabel, double line,
                                                      Double seasonAvg3pm, Double season3pPct,
                                                      int seasonHits, int seasonTotal) {
        List<Double> values = logs.stream()
            .map(g -> getStatValue(g, statKey).doubleValue())
            .toList();
        int n = values.size();
        int n5 = Math.min(5, n);
        double last10Avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double last5Avg = values.stream().limit(5).mapToDouble(Double::doubleValue).average().orElse(0);
        int hit10 = (int) values.stream().filter(v -> v >= line).count();
        int over5 = (int) values.stream().limit(5).filter(v -> v >= line).count();
        double stdDev = stdDev(values);
        boolean highVariance = n > 1 && stdDev > 0.3 * last10Avg;
        double maxLast5 = values.stream().limit(5).mapToDouble(Double::doubleValue).max().orElse(0);
        boolean oneBigGame = n5 >= 3 && maxLast5 > last5Avg + 2 * stdDev(values.stream().limit(5).toList());
        String varianceNote = oneBigGame ? "One big game in last 5 — tread carefully"
            : (highVariance ? "High variance" : "Consistent");

        boolean hasSeasonData = seasonTotal >= 10;
        double seasonHitRate = hasSeasonData ? (double) seasonHits / seasonTotal : 0.0;

        // Direction: determined by last-10 hit rate (primary signal).
        // Projection only breaks a 5/5 tie.
        int under10 = 10 - hit10;
        int under5 = 5 - over5;
        String suggestion;
        if (hit10 > under10) {
            suggestion = "Over";
        } else if (under10 > hit10) {
            suggestion = "Under";
        } else {
            // 5/5 tie — fall back to weighted projection
            double recentProjection = 0.6 * last5Avg + 0.4 * last10Avg;
            // fg3m hot take: season anchor only fires when the player actually hits the line
            // with reasonable frequency (≥35%) — avoids false Over for rare 3PT shooters
            if ("fg3m".equals(statKey) && seasonAvg3pm != null && seasonAvg3pm > 0
                    && (!hasSeasonData || seasonHitRate >= 0.35)
                    && recentProjection < seasonAvg3pm - 1.0) {
                varianceNote = "Hot take: cold recently, season avg suggests Over";
                return new PropPickSuggestionDto(propLabel, statKey, line, "Over", "Hot take",
                    Math.round(last10Avg * 10) / 10.0, Math.round(last5Avg * 10) / 10.0, "",
                    hit10, over5, varianceNote);
            }
            suggestion = recentProjection >= line ? "Over" : "Under";
            return new PropPickSuggestionDto(propLabel, statKey, line, suggestion, "Low",
                Math.round(last10Avg * 10) / 10.0, Math.round(last5Avg * 10) / 10.0, "",
                hit10, over5, varianceNote);
        }

        // Hits in favour of the chosen direction
        int hitsFor10 = "Over".equals(suggestion) ? hit10 : under10;
        int hitsFor5 = "Over".equals(suggestion) ? over5 : under5;
        double seasonRateFor = "Over".equals(suggestion) ? seasonHitRate
            : (hasSeasonData ? 1.0 - seasonHitRate : 0.0);

        // Confidence: hit rate is the primary driver.
        // Variance can only downgrade in the ambiguous 6-7/10 range.
        // A hit rate of 8+/10 overrides variance (the player is simply consistent against this line).
        String confidence;
        if (hitsFor10 >= 9) {
            // 9 or 10 out of 10 — dominant consistency; oneBigGame can't inflate Under counts
            confidence = (oneBigGame && "Over".equals(suggestion)) ? "Medium" : "High";
        } else if (hitsFor10 == 8) {
            // Strong but not perfect: season rate can rescue from a variance downgrade
            boolean seasonOverrides = hasSeasonData && seasonRateFor >= 0.75;
            confidence = (!highVariance || seasonOverrides) ? "High" : "Medium";
        } else if (hitsFor10 == 7) {
            // Decent: need recent form (≥4/5) AND low variance for High
            confidence = (hitsFor5 >= 4 && !highVariance) ? "High" : "Medium";
        } else {
            // 6/10: ambiguous enough that variance is a meaningful signal
            confidence = "Medium";
        }

        // fg3m cold-streak hot take: player's last-10 says Under but season shows they
        // normally clear this line — only fires when season hit rate is meaningful (≥35%)
        if ("fg3m".equals(statKey) && "Under".equals(suggestion)
                && seasonAvg3pm != null && seasonAvg3pm > 0
                && hasSeasonData && seasonHitRate >= 0.35) {
            double recentProjection = 0.6 * last5Avg + 0.4 * last10Avg;
            if (recentProjection < seasonAvg3pm - 1.0) {
                varianceNote = "Hot take: cold recently, season avg suggests Over";
                return new PropPickSuggestionDto(propLabel, statKey, line, "Over", "Hot take",
                    Math.round(last10Avg * 10) / 10.0, Math.round(last5Avg * 10) / 10.0, "",
                    hit10, over5, varianceNote);
            }
        }

        return new PropPickSuggestionDto(propLabel, statKey, line, suggestion, confidence,
            Math.round(last10Avg * 10) / 10.0, Math.round(last5Avg * 10) / 10.0, "",
            hit10, over5, varianceNote);
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
