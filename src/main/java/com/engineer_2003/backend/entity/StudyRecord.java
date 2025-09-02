package com.engineer_2003.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    //  프론트에서 필요로 하는 추가 필드
    @Column(nullable = false, length = 50)
    private String levelName;

    @Column(nullable = false, length = 50)
    private String partOfSpeech;

    @Column(nullable = false)
    private int wordCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
