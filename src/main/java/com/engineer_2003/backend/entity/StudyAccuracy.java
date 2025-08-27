package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "study_accuracy")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyAccuracy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accuracyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public double getAccuracyRate() {
        if (totalQuestions == 0) return 0.0;
        return (double) correctAnswers / totalQuestions * 100;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}