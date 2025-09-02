package com.engineer_2003.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.engineer_2003.backend.dto.StudyRecordResponseDto;
import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.repository.StudyRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyRecordService {

    private final StudyRecordRepository studyRecordRepository;

    public StudyRecord saveRecord(StudyRecord record) {
        return studyRecordRepository.save(record);
    }

    public List<StudyRecord> getRecordsByUser(Long userId) {
        return studyRecordRepository.findByUserUserId(userId);
    }

    // ✅ DTO 변환 메소드
    public List<StudyRecordResponseDto> getRecordsDtoByUser(Long userId) {
        return studyRecordRepository.findByUserUserId(userId).stream()
                .map(r -> new StudyRecordResponseDto(
                        r.getStudyDate(),
                        r.getLevelName(),
                        r.getPartOfSpeech(),
                        r.getWordCount(),
                        r.getStudyTime()
                ))
                .collect(Collectors.toList());
    }
}
