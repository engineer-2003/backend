package com.engineer_2003.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.engineer_2003.backend.entity.StudyRecord;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    List<StudyRecord> findByUserUserId(Long userId); // 특정 유저 기록 조회
    List<StudyRecord> findByUserUserIdAndStudyDate(Long userId, LocalDate studyDate); // 특정 일자 기록
}
