package io.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "study_accuracy")
public class StudyAccuracy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accuracyId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Transient
    private double accuracyRate; // DB에 계산된 칼럼 대신 엔티티에서 계산

    public double getAccuracyRate() {
        if (totalQuestions == 0) return 0.0;
        return (double) correctAnswers / totalQuestions * 100;
    }

    @Column(nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();
}
