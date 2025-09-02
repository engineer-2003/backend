package com.engineer_2003.backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudyRecordResponseDto {
    private LocalDate studyDate;
    private String levelName;
    private String partOfSpeech;
    private int wordCount;
    private int studyTime; // 분 단위
}
