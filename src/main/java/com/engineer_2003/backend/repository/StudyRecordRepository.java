// StudyRecordRepository.java
package com.engineer_2003.backend.repository;

import com.engineer_2003.backend.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findByUserIdOrderByStudyDateDesc(Long userId);

    @Query("SELECT sr FROM StudyRecord sr WHERE sr.userId = :userId " +
            "AND sr.studyDate BETWEEN :startDate AND :endDate " +
            "ORDER BY sr.studyDate DESC")
    List<StudyRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(sr.studyTime), 0) FROM StudyRecord sr " +
            "WHERE sr.userId = :userId AND sr.studyDate BETWEEN :startDate AND :endDate")
    Integer getTotalStudyTimeByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}