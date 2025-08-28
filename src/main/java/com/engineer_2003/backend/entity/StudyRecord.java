package com.engineer_2003.backend.entity;

import java.time.LocalDate;

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

    @ManyToOne(fetch = FetchType.LAZY) // 유저별 학습 기록
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate studyDate; // 학습 일자

    @Column(nullable = false, length = 10)
    private String levelName; // 급수 (예: N5, N3 등)

    @Column(nullable = false, length = 20)
    private String partOfSpeech; // 품사 (예: 명사, 동사 등)

    @Column(nullable = false)
    private int wordCount; // 학습한 단어 수

    @Column(nullable = false)
    private int studyTime; // 학습 시간 (분 단위)

    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}
