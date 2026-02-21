package com.nbastats.app.web;

import com.nbastats.app.dto.TodayPickDto;
import com.nbastats.app.service.DailyPropLineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TodayPicksController {

    private final DailyPropLineService dailyPropLineService;

    public TodayPicksController(DailyPropLineService dailyPropLineService) {
        this.dailyPropLineService = dailyPropLineService;
    }

    @GetMapping("/today-picks")
    public List<TodayPickDto> getTodayPicks(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate useDate = date != null ? date : dailyPropLineService.getLatestLineDate().orElse(null);
        if (useDate == null) return List.of();
        return dailyPropLineService.getTodayPicks(useDate);
    }

    @PostMapping("/admin/daily-lines")
    public ResponseEntity<Map<String, Object>> addDailyLines(@RequestBody AddDailyLinesRequest request) {
        if (request.getDate() == null || request.getDate().isBlank() || request.getLines() == null || request.getLines().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "date and lines required"));
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.getDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "date must be YYYY-MM-DD"));
        }
        String season = request.getSeason() != null ? request.getSeason() : "2025-26";
        int saved = dailyPropLineService.addDailyLines(date, season, request.getLines());
        return ResponseEntity.ok(Map.of("saved", saved, "date", date.toString()));
    }

    public static class AddDailyLinesRequest {
        private String date;
        private String season;
        private List<DailyPropLineService.LineInput> lines;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }
        public List<DailyPropLineService.LineInput> getLines() { return lines; }
        public void setLines(List<DailyPropLineService.LineInput> lines) { this.lines = lines; }
    }
}
