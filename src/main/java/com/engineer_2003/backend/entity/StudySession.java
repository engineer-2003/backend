// StudySession.java
package com.engineer_2003.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "level_name", nullable = false)
    private String levelName;

    @Column(name = "part_of_speech", nullable = false)
    private String partOfSpeech;

    @Column(name = "study_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyType studyType;

    @Column(name = "start_time")
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_questions")
    private Integer totalQuestions = 0;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "is_retry_session")
    private Boolean isRetrySession = false;

    public enum StudyType {
        SENTENCE, CONJUGATION
    }
}