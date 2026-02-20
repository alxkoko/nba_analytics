package com.nbastats.app.repository;

import com.nbastats.app.entity.DailyPropLine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DailyPropLineRepository extends JpaRepository<DailyPropLine, Long> {

    @Query("SELECT d FROM DailyPropLine d JOIN FETCH d.player WHERE d.lineDate = :date ORDER BY CASE d.confidence WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 ELSE 3 END, d.id")
    List<DailyPropLine> findByLineDateWithPlayer(LocalDate date);
}
