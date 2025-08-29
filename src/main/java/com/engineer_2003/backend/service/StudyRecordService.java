package com.engineer_2003.backend.service;

import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.repository.StudyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
