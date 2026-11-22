// WordStudyResult.java
package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_study_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "word_id", "study_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordStudyResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "study_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyType studyType;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    @Builder.Default
    @Column(name = "attempt_count")
    private Integer attemptCount = 1;

    @Builder.Default
    @Column(name = "incorrect_count", nullable = false)
    private Integer incorrectCount = 0;

    @Builder.Default
    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", insertable = false, updatable = false)
    private Word word;

    public enum StudyType {
        SENTENCE, CONJUGATION
    }
}