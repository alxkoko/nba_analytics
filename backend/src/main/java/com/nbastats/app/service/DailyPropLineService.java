package com.nbastats.app.service;

import com.nbastats.app.dto.PropPickSuggestionDto;
import com.nbastats.app.dto.TodayPickDto;
import com.nbastats.app.entity.DailyPropLine;
import com.nbastats.app.entity.Player;
import com.nbastats.app.repository.DailyPropLineRepository;
import com.nbastats.app.repository.PlayerGameLogRepository;
import com.nbastats.app.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyPropLineService {

    private final PlayerRepository playerRepository;
    private final DailyPropLineRepository dailyPropLineRepository;
    private final PlayerGameLogRepository playerGameLogRepository;
    private final PlayerService playerService;

    public DailyPropLineService(PlayerRepository playerRepository,
                                DailyPropLineRepository dailyPropLineRepository,
                                PlayerGameLogRepository playerGameLogRepository,
                                PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.dailyPropLineRepository = dailyPropLineRepository;
        this.playerGameLogRepository = playerGameLogRepository;
        this.playerService = playerService;
    }

    /** Add lines for a date. Each line: player name (matched to our DB), stat key, line value. We compute suggestion and store. */
    @Transactional
    public int addDailyLines(LocalDate date, String season, List<LineInput> lines) {
        int saved = 0;
        for (LineInput input : lines) {
            if (input.getPlayer() == null || input.getStat() == null || input.getLine() == null) continue;
            List<Player> matches = playerRepository.searchByName(input.getPlayer().trim());
            if (matches.isEmpty()) continue;
            Player player = matches.get(0);
            String statKey = playerService.getAllowedStats().contains(input.getStat().trim().toLowerCase())
                ? input.getStat().trim().toLowerCase() : "pts";
            double lineVal = input.getLine().doubleValue();
            PropPickSuggestionDto dto = playerService.getSinglePropSuggestion(player.getId(), season, statKey, lineVal);
            if (dto == null) continue;
            DailyPropLine entity = new DailyPropLine();
            entity.setPlayer(player);
            entity.setStatKey(statKey);
            entity.setLineValue(lineVal);
            entity.setLineDate(date);
            entity.setSuggestion(dto.suggestion());
            entity.setConfidence(dto.confidence());
            entity.setReason(String.format("%d/5 last 5, %d/10 last 10. %s", dto.overLast5(), dto.hitRateLast10(), dto.varianceNote()));
            entity.setHitRateLast10(dto.hitRateLast10());
            entity.setOverLast5(dto.overLast5());
            dailyPropLineRepository.save(entity);
            saved++;
        }
        return saved;
    }

    private static final int TOP_PICKS_LIMIT = 3;

    /** Confidence order for "most probable": High first, then Medium, then Low. */
    private static int confidenceOrder(String c) {
        if (c == null) return 0;
        return switch (c) {
            case "High" -> 3;
            case "Medium" -> 2;
            case "Low" -> 1;
            default -> 0;
        };
    }

    /** For sort: Over = hit rate (higher better); Under = 10 - hit rate (more unders = better). -1 when missing so those sort last. */
    private static int normalizedHitRate(TodayPickDto t) {
        int h = t.hitRateLast10();
        if (h < 0) return -1;
        return "Under".equalsIgnoreCase(t.suggestion()) ? (10 - h) : h;
    }

    /** For sort: Over = over_last_5 (higher better); Under = 5 - over_last_5 (more unders = better). -1 when missing so those sort last. */
    private static int normalizedOver5(TodayPickDto t) {
        int o = t.overLast5();
        if (o < 0) return -1;
        return "Under".equalsIgnoreCase(t.suggestion()) ? (5 - o) : o;
    }

    /** Latest line_date in the DB (most recent day with any picks). */
    public Optional<LocalDate> getLatestLineDate() {
        return dailyPropLineRepository.findMaxLineDate();
    }

    /** NBA season for a date: Oct–June = current season; July–Sep = previous. */
    private static String seasonForDate(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int startYear = month >= 10 ? year : year - 1;
        int endYY = (startYear + 1) % 100;
        return startYear + "-" + (endYY < 10 ? "0" : "") + endYY;
    }

    /** Get picks for a date, sorted by confidence (High first). limit null = 3 (homepage), 0 or negative = all. */
    public List<TodayPickDto> getTodayPicks(LocalDate date, Integer limit) {
        List<DailyPropLine> list = dailyPropLineRepository.findByLineDateWithPlayer(date);
        List<TodayPickDto> out = new ArrayList<>();
        if (list.isEmpty()) return out;

        List<Long> playerIds = list.stream().map(d -> d.getPlayer().getId()).distinct().toList();
        String season = seasonForDate(date);
        Set<Long> eligiblePlayerIds = new HashSet<>(playerGameLogRepository.findPlayerIdsWithAtLeast10GamesBefore(playerIds, season, date));
        List<Object[]> teamRows = playerIds.isEmpty() ? List.of() : playerGameLogRepository.findLatestTeamAbbrByPlayerIdsAndSeason(playerIds, season);
        Map<Long, String> playerToTeam = new LinkedHashMap<>();
        for (Object[] row : teamRows) {
            Long pid = ((Number) row[0]).longValue();
            String abbr = row[1] != null ? row[1].toString() : null;
            playerToTeam.putIfAbsent(pid, abbr);
        }

        for (DailyPropLine d : list) {
            if (!eligiblePlayerIds.contains(d.getPlayer().getId())) continue;
            String statLabel = PlayerService.getStatLabel(d.getStatKey());
            String teamAbbr = playerToTeam.get(d.getPlayer().getId());
            int hit10 = d.getHitRateLast10() != null ? d.getHitRateLast10() : -1;
            int over5 = d.getOverLast5() != null ? d.getOverLast5() : -1;
            out.add(new TodayPickDto(
                d.getId(),
                d.getPlayer().getFullName(),
                d.getPlayer().getId(),
                statLabel,
                d.getStatKey(),
                d.getLineValue(),
                d.getSuggestion(),
                d.getConfidence(),
                d.getReason(),
                teamAbbr,
                hit10,
                over5
            ));
        }
        // Sort by confidence (High first), then suggestion-aware strength: Over = higher hit rate, Under = more unders (10-h, 5-o).
        out.sort(Comparator
            .comparingInt((TodayPickDto t) -> confidenceOrder(t.confidence())).reversed()
            .thenComparing(Comparator.comparingInt((TodayPickDto t) -> normalizedHitRate(t)).reversed())
            .thenComparing(Comparator.comparingInt((TodayPickDto t) -> normalizedOver5(t)).reversed())
            .thenComparingLong(TodayPickDto::id));
            
        if (limit != null) {
            int cap = limit <= 0 ? out.size() : Math.min(limit, out.size());
            return out.size() <= cap ? out : out.subList(0, cap);
        }

        // Homepage: first 3 (by confidence/id), then +1 fg3m (if available), +1 reb_ast (if available)
        List<TodayPickDto> result = new ArrayList<>(out.size() <= TOP_PICKS_LIMIT ? out : out.subList(0, TOP_PICKS_LIMIT));
        Set<Long> resultIds = result.stream().map(TodayPickDto::id).collect(Collectors.toSet());
        for (TodayPickDto t : out) {
            if (resultIds.contains(t.id())) continue;
            if ("fg3m".equals(t.statKey())) {
                result.add(t);
                resultIds.add(t.id());
                break;
            }
        }
        for (TodayPickDto t : out) {
            if (resultIds.contains(t.id())) continue;
            if ("reb_ast".equals(t.statKey())) {
                result.add(t);
                break;
            }
        }
        return result;
    }

    /** Request body for one line when adding daily lines. */
    public static class LineInput {
        private String player;
        private String stat;
        private Number line;

        public String getPlayer() { return player; }
        public void setPlayer(String player) { this.player = player; }
        public String getStat() { return stat; }
        public void setStat(String stat) { this.stat = stat; }
        public Number getLine() { return line; }
        public void setLine(Number line) { this.line = line; }
    }
}
