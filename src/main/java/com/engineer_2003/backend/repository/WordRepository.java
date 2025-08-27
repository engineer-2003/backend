package com.engineer_2003.backend.repository;

import com.engineer_2003.backend.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByLevelNameAndPartOfSpeech(String levelName, String partOfSpeech);

    @Query("SELECT w FROM Word w LEFT JOIN FETCH w.adjectiveForms LEFT JOIN FETCH w.verbForms WHERE w.levelName = :levelName AND w.partOfSpeech = :partOfSpeech ORDER BY w.id")
    List<Word> findWordsWithFormsbyLevelAndPart(@Param("levelName") String levelName, @Param("partOfSpeech") String partOfSpeech);

    @Query("SELECT w FROM Word w LEFT JOIN FETCH w.adjectiveForms LEFT JOIN FETCH w.verbForms WHERE w.id = :id")
    Word findWordWithFormsById(@Param("id") Long id);
}