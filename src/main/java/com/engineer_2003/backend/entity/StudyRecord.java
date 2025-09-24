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
    private Integer studyTime = 0; // 초 단위로 변경

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

    /**
     * 학습 시간을 포맷팅된 문자열로 반환
     * @return "1시간 30분 45초" 형태의 문자열
     */
    public String getFormattedStudyTime() {
        if (studyTime == null || studyTime == 0) {
            return "0초";
        }

        int hours = studyTime / 3600;
        int minutes = (studyTime % 3600) / 60;
        int seconds = studyTime % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("시간 ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("분 ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("초");
        }

        return sb.toString().trim();
    }
}