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
                        @Param("endDate") LocalDate endDate);

        /**
         * 특정 기간 동안의 총 학습 시간 조회 (초 단위)
         */
        @Query("SELECT COALESCE(SUM(sr.studyTime), 0) FROM StudyRecord sr " +
                        "WHERE sr.userId = :userId AND sr.studyDate BETWEEN :startDate AND :endDate")
        Integer getTotalStudyTimeByUserIdAndDateRange(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 특정 날짜의 학습 시간 조회 (초 단위)
         */
        @Query("SELECT COALESCE(SUM(sr.studyTime), 0) FROM StudyRecord sr " +
                        "WHERE sr.userId = :userId AND sr.studyDate = :studyDate")
        Integer getStudyTimeByUserIdAndDate(
                        @Param("userId") Long userId,
                        @Param("studyDate") LocalDate studyDate);

        /**
         * 최근 N일간의 일별 학습 시간 조회 (초 단위)
         */
        @Query("SELECT sr.studyDate, COALESCE(SUM(sr.studyTime), 0) as totalTime " +
                        "FROM StudyRecord sr " +
                        "WHERE sr.userId = :userId AND sr.studyDate BETWEEN :startDate AND :endDate " +
                        "GROUP BY sr.studyDate " +
                        "ORDER BY sr.studyDate DESC")
        List<Object[]> getDailyStudyTimeByUserIdAndDateRange(
                        @Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 특정 날짜에 학습 기록이 존재하는지 확인
         */
        boolean existsByUserIdAndStudyDate(Long userId, LocalDate studyDate);
}