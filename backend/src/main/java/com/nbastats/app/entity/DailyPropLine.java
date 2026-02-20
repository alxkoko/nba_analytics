package com.nbastats.app.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_prop_lines")
public class DailyPropLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "stat_key", nullable = false, length = 32)
    private String statKey;

    @Column(name = "line_value", nullable = false)
    private Double lineValue;

    @Column(name = "line_date", nullable = false)
    private LocalDate lineDate;

    @Column(name = "suggestion", nullable = false, length = 8)
    private String suggestion;

    @Column(name = "confidence", nullable = false, length = 16)
    private String confidence;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public String getStatKey() { return statKey; }
    public void setStatKey(String statKey) { this.statKey = statKey; }

    public Double getLineValue() { return lineValue; }
    public void setLineValue(Double lineValue) { this.lineValue = lineValue; }

    public LocalDate getLineDate() { return lineDate; }
    public void setLineDate(LocalDate lineDate) { this.lineDate = lineDate; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
