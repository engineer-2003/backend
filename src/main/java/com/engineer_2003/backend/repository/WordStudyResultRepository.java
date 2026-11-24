// WordStudyResultRepository.java
package com.engineer_2003.backend.repository;

import com.engineer_2003.backend.entity.WordStudyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordStudyResultRepository extends JpaRepository<WordStudyResult, Long> {

        Optional<WordStudyResult> findByUserIdAndWordIdAndStudyType(
                        Long userId, Long wordId, WordStudyResult.StudyType studyType);

        List<WordStudyResult> findByUserIdAndStudyType(
                        Long userId, WordStudyResult.StudyType studyType);

        @Query("SELECT wsr FROM WordStudyResult wsr " +
                        "JOIN FETCH wsr.word w " +
                        "WHERE wsr.userId = :userId " +
                        "AND wsr.isCorrect = false " +
                        "AND w.levelName = :levelName " +
                        "AND w.partOfSpeech = :partOfSpeech " +
                        "AND wsr.studyType = :studyType")
        List<WordStudyResult> findIncorrectWordsByUserAndLevelAndPartAndType(
                        @Param("userId") Long userId,
                        @Param("levelName") String levelName,
                        @Param("partOfSpeech") String partOfSpeech,
                        @Param("studyType") WordStudyResult.StudyType studyType);

        @Query("SELECT COUNT(wsr) FROM WordStudyResult wsr " +
                        "JOIN wsr.word w " +
                        "WHERE wsr.userId = :userId " +
                        "AND wsr.isCorrect = false " +
                        "AND w.levelName = :levelName " +
                        "AND w.partOfSpeech = :partOfSpeech " +
                        "AND wsr.studyType = :studyType")
        Long countIncorrectWordsByUserAndLevelAndPartAndType(
                        @Param("userId") Long userId,
                        @Param("levelName") String levelName,
                        @Param("partOfSpeech") String partOfSpeech,
                        @Param("studyType") WordStudyResult.StudyType studyType);

        List<WordStudyResult> findByUserIdAndIsCorrectFalse(Long userId);

        /**
         * 사용자가 실제로 학습한 단어 목록 조회
         * Word를 JOIN FETCH하여 LazyInitializationException 방지
         * 최근 학습한 단어부터 정렬
         */
        @Query("SELECT wsr FROM WordStudyResult wsr " +
                        "JOIN FETCH wsr.word w " +
                        "WHERE wsr.userId = :userId " +
                        "ORDER BY wsr.lastStudiedAt DESC")
        List<WordStudyResult> findRecentWordsByUserId(
                        @Param("userId") Long userId);
}