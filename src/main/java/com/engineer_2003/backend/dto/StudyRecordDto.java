// StudyRecordDto.java
package com.engineer_2003.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecordDto {
    private String studyDate;
    private String levelName;
    private String partOfSpeech;
    private Integer wordCount;
    private Integer studyTime;
}