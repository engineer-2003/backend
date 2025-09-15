// StudyRecord.java
package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Column(name = "part_of_speech", nullable = false)
    private String partOfSpeech;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Column(name = "study_time", nullable = false)
    private Integer studyTime = 0; // 분 단위

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "study_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyType studyType = StudyType.SENTENCE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum StudyType {
        SENTENCE, CONJUGATION
    }
}