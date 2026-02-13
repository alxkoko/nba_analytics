package com.nbastats.app.web;

import com.nbastats.app.dto.GameLogDto;
import com.nbastats.app.dto.OverUnderDto;
import com.nbastats.app.dto.SeasonStatsDto;
import com.nbastats.app.entity.Player;
import com.nbastats.app.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public List<Player> search(@RequestParam(value = "q", required = false) String q) {
        return playerService.searchByName(q != null ? q : "");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getById(@PathVariable Long id) {
        Player p = playerService.getById(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/games")
    public List<GameLogDto> getGames(
        @PathVariable Long id,
        @RequestParam(value = "season", defaultValue = "2024-25") String season
    ) {
        return playerService.getGameLog(id, season);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<SeasonStatsDto> getStats(
        @PathVariable Long id,
        @RequestParam(value = "season", defaultValue = "2024-25") String season
    ) {
        SeasonStatsDto stats = playerService.getSeasonStats(id, season);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/over-under")
    public ResponseEntity<OverUnderDto> getOverUnder(
        @PathVariable Long id,
        @RequestParam(value = "season", defaultValue = "2024-25") String season,
        @RequestParam(value = "stat", defaultValue = "pts") String stat,
        @RequestParam(value = "threshold", defaultValue = "25.0") double threshold,
        @RequestParam(value = "lastN", required = false) Integer lastN
    ) {
        OverUnderDto dto = playerService.getOverUnder(id, season, stat, threshold, lastN);
        return ResponseEntity.ok(dto);
    }
}
