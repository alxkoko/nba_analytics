package com.nbastats.app.web;

import com.nbastats.app.service.SeasonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/season")
@CrossOrigin(origins = "*")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/current")
    public Map<String, String> getCurrent() {
        return Map.of("season", seasonService.getCurrentSeason());
    }

    @GetMapping("/list")
    public List<String> getSeasons(@RequestParam(value = "count", defaultValue = "5") int count) {
        return seasonService.getSeasons(Math.min(Math.max(count, 1), 20));
    }
}
