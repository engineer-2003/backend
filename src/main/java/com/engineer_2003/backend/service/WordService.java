package com.engineer_2003.backend.service;

import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@Transactional
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    public List<WordDto> getWordsByLevelAndPart(String levelName, String partOfSpeech) {
        List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(levelName, partOfSpeech);
        List<WordDto> wordDtos = words.stream()
                .map(WordDto::new)
                .collect(Collectors.toList());

        // 랜덤하게 섞기
        Collections.shuffle(wordDtos);
        return wordDtos;
    }

    public WordDto getWordById(Long id) {
        Word word = wordRepository.findWordWithFormsById(id);
        return word != null ? new WordDto(word) : null;
    }

    public boolean checkAnswer(Long wordId, String answer, String formType) {
        Word word = wordRepository.findWordWithFormsById(wordId);
        if (word == null) return false;

        WordDto wordDto = new WordDto(word);

        // 기본 단어 확인
        if ("word".equals(formType)) {
            return word.getWord().equals(answer) || word.getReading().equals(answer);
        }

        // 활용형 확인
        if (wordDto.getForms().containsKey(formType)) {
            return wordDto.getForms().get(formType).equals(answer);
        }

        return false;
    }
}