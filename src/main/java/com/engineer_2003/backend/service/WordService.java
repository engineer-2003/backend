package com.engineer_2003.backend.service;

import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WordService {

    private static final Logger logger = LoggerFactory.getLogger(WordService.class);

    @Autowired
    private WordRepository wordRepository;

    public List<WordDto> getWordsByLevelAndPart(String levelName, String partOfSpeech) {
        try {
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(levelName, partOfSpeech);
            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try { return new WordDto(word); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .filter(dto -> {
                        if (("동사".equals(partOfSpeech) || "형용사".equals(partOfSpeech))) {
                            return dto.getForms() != null && !dto.getForms().isEmpty();
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            Collections.shuffle(wordDtos);
            return wordDtos;
        } catch (Exception e) {
            throw new RuntimeException("단어 목록 조회 실패", e);
        }
    }

    public WordDto getWordById(Long id) {
        Word word = wordRepository.findWordWithFormsById(id);
        return word != null ? new WordDto(word) : null;
    }

    public Map<String, Object> generateSentenceProblem(Long wordId) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return null;

            Map<String, Object> problem = new HashMap<>();
            problem.put("id", word.getId());
            problem.put("meaning", word.getMeaning());
            problem.put("exampleTranslation", word.getExampleTranslation());
            problem.put("level", word.getLevelName());
            problem.put("part", word.getPartOfSpeech());

            String blankSentence = createBlankSentence(word.getExampleSentence(), word.getWord(), word.getReading());
            problem.put("blankSentence", blankSentence);

            problem.put("correctAnswers", Arrays.asList(word.getWord(), word.getReading()));
            problem.put("hint", getHint(word.getReading()));

            return problem;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> generateFormProblem(Long wordId, String formType) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return null;

            WordDto wordDto = new WordDto(word);

            // 동사나 형용사가 아니면 활용형 문제를 생성할 수 없음
            if (!"동사".equals(word.getPartOfSpeech()) && !"형용사".equals(word.getPartOfSpeech())) {
                return null;
            }

            String selectedForm;

            // formType이 지정되어 있고 유효하다면 그 활용형을 선택
            if (formType != null && !formType.trim().isEmpty() && wordDto.getForms().containsKey(formType)) {
                selectedForm = formType;
            } else {
                // formType이 없으면 기존처럼 랜덤 선택
                List<String> availableForms = getAvailableForms(wordId);
                if (availableForms.isEmpty()) return null;
                Random random = new Random();
                selectedForm = availableForms.get(random.nextInt(availableForms.size()));
            }

            String correctAnswer = wordDto.getForms().get(selectedForm);

            Map<String, Object> problem = new HashMap<>();
            problem.put("id", word.getId());
            problem.put("word", word.getWord());
            problem.put("reading", word.getReading());
            problem.put("meaning", word.getMeaning());
            problem.put("part", word.getPartOfSpeech());
            problem.put("level", word.getLevelName());
            problem.put("formType", selectedForm);
            problem.put("formName", getFormDisplayName(selectedForm));
            problem.put("correctAnswer", correctAnswer);
            problem.put("hint", getHint(correctAnswer));

            return problem;
        } catch (Exception e) {
            logger.error("활용형 문제 생성 중 오류 발생 - 단어 ID: {}", wordId, e);
            return null;
        }
    }

    private String createBlankSentence(String sentence, String word, String reading) {
        if (sentence == null || sentence.trim().isEmpty()) return "例文がありません。";

        String blankSentence = sentence;
        if (word != null && !word.trim().isEmpty()) {
            blankSentence = blankSentence.replace(word, "（　）");
        }
        if (reading != null && !reading.trim().isEmpty()) {
            blankSentence = blankSentence.replace(reading, "（　）");
        }
        return blankSentence;
    }

    private String getHint(String answer) {
        if (answer == null || answer.isEmpty()) return "";
        return answer.length() == 1 ? "힌트: " + answer : "힌트: " + answer.charAt(0) + "...";
    }

    public boolean checkAnswer(Long wordId, String answer, String formType) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return false;

            WordDto wordDto = new WordDto(word);
            String trimmedAnswer = answer.trim();

            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord().equals(trimmedAnswer) || word.getReading().equals(trimmedAnswer);
            }

            if (wordDto.getForms().containsKey(formType)) {
                String correctAnswer = wordDto.getForms().get(formType);
                if (correctAnswer == null) return false;

                if (correctAnswer.equals(trimmedAnswer)) return true;

                if (word.getWord().length() > 0 && word.getReading().length() > 0) {
                    char firstKanji = word.getWord().charAt(0);
                    char firstKana = word.getReading().charAt(0);

                    if (correctAnswer.charAt(0) == firstKanji && isKanji(firstKanji)) {
                        String pseudoHiraganaAnswer = correctAnswer.replaceFirst(
                                String.valueOf(firstKanji),
                                String.valueOf(firstKana)
                        );
                        if (pseudoHiraganaAnswer.equals(trimmedAnswer)) return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isKanji(char c) {
        return (c >= 0x4E00 && c <= 0x9FBF);
    }

    public String getCorrectAnswer(Long wordId, String formType) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return "";
            WordDto wordDto = new WordDto(word);

            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord();
            }
            if (wordDto.getForms().containsKey(formType)) {
                return wordDto.getForms().get(formType);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public List<String> getAvailableForms(Long wordId) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return Collections.emptyList();

            WordDto wordDto = new WordDto(word);
            return new ArrayList<>(wordDto.getForms().keySet());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getFormDisplayName(String formType) {
        Map<String, String> formNames = new HashMap<>();

        // 동사
        formNames.put("masuForm", "ます형");
        formNames.put("negativeForm", "부정형");
        formNames.put("volitionalForm", "청유형");
        formNames.put("conditionalForm", "가정형");
        formNames.put("imperativeForm", "명령형");
        formNames.put("potentialForm", "가능형");

        // 형용사
        formNames.put("mizenForm", "미연형");
        formNames.put("teForm", "て형");
        formNames.put("pastForm", "과거형");
        formNames.put("politeForm", "정중형");

        return formNames.getOrDefault(formType, formType);
    }
}