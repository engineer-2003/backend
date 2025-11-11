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
            @Param("studyType") WordStudyResult.StudyType studyType
    );

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
            @Param("studyType") WordStudyResult.StudyType studyType
    );

    @Query("SELECT COUNT(DISTINCT wsr.word.id) FROM WordStudyResult wsr WHERE wsr.userId = :userId")
    Long countDistinctWordsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT wsr.word.id) FROM WordStudyResult wsr WHERE wsr.userId = :userId AND wsr.word.levelName = :level")
    Long countDistinctWordsByUserIdAndLevel(@Param("userId") Long userId, @Param("level") String level);

    @Query("SELECT COUNT(DISTINCT wsr.word.id) FROM WordStudyResult wsr WHERE wsr.userId = :userId AND wsr.word.levelName = :level AND wsr.word.partOfSpeech = :part")
    Long countDistinctWordsByUserIdAndLevelAndPart(@Param("userId") Long userId, @Param("level") String level, @Param("part") String part);

    List<WordStudyResult> findByUserIdAndIsCorrectFalse(Long userId);
}