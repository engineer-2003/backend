package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "study_records")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false)
    private int studyTime;   // 분 단위

    @Column(nullable = false)
    private int totalYearTime;

    @Column(nullable = false)
    private int totalMonthTime;

    @Column(nullable = false)
    private int totalDayTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}