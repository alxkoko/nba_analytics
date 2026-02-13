package com.nbastats.app.repository;

import com.nbastats.app.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query("SELECT p FROM Player p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY p.fullName")
    List<Player> searchByName(@Param("q") String query);
}
