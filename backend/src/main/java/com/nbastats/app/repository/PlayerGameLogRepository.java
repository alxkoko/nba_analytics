package com.nbastats.app.repository;

import com.nbastats.app.entity.PlayerGameLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerGameLogRepository extends JpaRepository<PlayerGameLog, Long> {

    List<PlayerGameLog> findByPlayer_IdAndSeasonOrderByGameDateDesc(Long playerId, String season);
}
