// WordService.java
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
        logger.debug("단어 조회 시작 - 레벨: {}, 품사: {}", levelName, partOfSpeech);

        try {
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(
                    levelName, partOfSpeech);

            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try {
                            return new WordDto(word);
                        } catch (Exception e) {
                            logger.warn("WordDto 변환 실패 - 단어 ID: {}", word.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(dto -> {
                        if ("동사".equals(partOfSpeech) || "형용사".equals(partOfSpeech)) {
                            return dto.getForms() != null && !dto.getForms().isEmpty();
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            Collections.shuffle(wordDtos);

            logger.info("단어 목록 조회 완료 - 레벨: {}, 품사: {}, 반환 개수: {}",
                    levelName, partOfSpeech, wordDtos.size());

            return wordDtos;

        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생 - 레벨: {}, 품사: {}",
                    levelName, partOfSpeech, e);
            throw new RuntimeException("단어 목록 조회 실패", e);
        }
    }

    public WordDto getWordById(Long id) {
        logger.debug("단어 조회 - ID: {}", id);

        try {
            Word word = wordRepository.findWordWithFormsById(id);

            if (word == null) {
                logger.info("단어를 찾을 수 없음 - ID: {}", id);
                return null;
            }

            return new WordDto(word);

        } catch (Exception e) {
            logger.error("단어 조회 중 오류 발생 - ID: {}", id, e);
            throw new RuntimeException("단어 조회 실패", e);
        }
    }

    /**
     * 예문 기반 문제 생성
     */
    public Map<String, Object> generateSentenceProblem(Long wordId) {
        logger.debug("예문 문제 생성 - 단어 ID: {}", wordId);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return null;
            }

            Map<String, Object> problem = new HashMap<>();

            problem.put("id", word.getId());
            problem.put("meaning", word.getMeaning());
            problem.put("exampleTranslation", word.getExampleTranslation());

            problem.put("level", word.getLevelName());
            problem.put("part", word.getPartOfSpeech());

            String blankSentence = createBlankSentence(
                    word.getExampleSentence(),
                    word.getWord(),
                    word.getReading());

            problem.put("blankSentence", blankSentence);
            problem.put("correctAnswers", Arrays.asList(
                    word.getWord(),
                    word.getReading()));
            problem.put("hint", getHint(word.getReading()));

            return problem;

        } catch (Exception e) {
            logger.error("예문 문제 생성 중 오류 발생 - 단어 ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 예문에 실제로 등장하는 활용형 하나를 선택해 문제 생성
     *
     * 예문에 활용형이 없으면 null 반환
     */
    public Map<String, Object> generateFormProblem(Long wordId) {
        logger.debug("예문 기반 활용형 문제 생성 - 단어 ID: {}", wordId);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return null;
            }

            if (!"동사".equals(word.getPartOfSpeech())
                    && !"형용사".equals(word.getPartOfSpeech())) {
                logger.warn("활용형 학습 대상 품사가 아님 - ID: {}, 품사: {}",
                        wordId, word.getPartOfSpeech());
                return null;
            }

            String exampleSentence = word.getExampleSentence();

            if (exampleSentence == null || exampleSentence.trim().isEmpty()) {
                logger.info("예문이 없어 활용형 문제를 건너뜀 - 단어 ID: {}", wordId);
                return null;
            }

            WordDto wordDto = new WordDto(word);

            Map<String, String> forms = wordDto.getForms();

            if (forms == null || forms.isEmpty()) {
                logger.info("활용형 데이터가 없어 문제를 건너뜀 - 단어 ID: {}", wordId);
                return null;
            }

            // 예문에 실제로 등장하는 활용형만 추출
            List<Map.Entry<String, String>> matchingForms = forms.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null)
                    .filter(entry -> entry.getValue() != null
                            && !entry.getValue().trim().isEmpty())
                    .filter(entry -> exampleSentence.contains(entry.getValue()))
                    .collect(Collectors.toList());

            if (matchingForms.isEmpty()) {
                logger.info("예문에서 활용형을 찾지 못해 건너뜀 - 단어 ID: {}, 단어: {}",
                        wordId, word.getWord());
                return null;
            }

            /*
             * 여러 활용형이 예문에 포함된 경우
             * 가장 긴 형태를 우선 후보로 사용합니다.
             *
             * 짧은 문자열이 다른 활용형의 일부로 잘못 선택되는 상황을
             * 줄이기 위한 처리입니다.
             */
            int maxLength = matchingForms.stream()
                    .mapToInt(entry -> entry.getValue().length())
                    .max()
                    .orElse(0);

            List<Map.Entry<String, String>> longestMatchingForms = matchingForms.stream()
                    .filter(entry -> entry.getValue().length() == maxLength)
                    .collect(Collectors.toList());

            // 후보 중 하나를 무작위 선택
            Random random = new Random();

            Map.Entry<String, String> selectedForm = longestMatchingForms.get(
                    random.nextInt(longestMatchingForms.size()));

            String formType = selectedForm.getKey();
            String correctAnswer = selectedForm.getValue();

            // 정답 활용형 부분만 빈칸으로 변경
            String blankSentence = exampleSentence.replace(
                    correctAnswer,
                    "（　）");

            Map<String, Object> problem = new HashMap<>();

            problem.put("id", word.getId());
            problem.put("word", word.getWord());
            problem.put("reading", word.getReading());
            problem.put("meaning", word.getMeaning());

            problem.put("part", word.getPartOfSpeech());
            problem.put("level", word.getLevelName());

            problem.put("formType", formType);
            problem.put("formName", getFormDisplayName(formType));
            problem.put("correctAnswer", correctAnswer);

            problem.put("blankSentence", blankSentence);
            problem.put("exampleSentence", exampleSentence);
            problem.put("exampleTranslation", word.getExampleTranslation());

            problem.put("hint", getHint(correctAnswer));

            logger.info("활용형 문제 생성 성공 - 단어 ID: {}, 단어: {}, 활용형: {}, 정답: {}",
                    wordId, word.getWord(), formType, correctAnswer);

            return problem;

        } catch (Exception e) {
            logger.error("활용형 문제 생성 중 오류 발생 - 단어 ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 활용형의 표시명 반환
     */
    private String getFormDisplayName(String formType) {
        Map<String, String> formNames = new HashMap<>();

        // 동사 활용형
        formNames.put("masuForm", "ます형");
        formNames.put("negativeForm", "부정형");
        formNames.put("volitionalForm", "의지형");
        formNames.put("conditionalForm", "조건형");
        formNames.put("imperativeForm", "명령형");
        formNames.put("potentialForm", "가능형");
        formNames.put("teForm", "て형");
        formNames.put("taForm", "た형");
        formNames.put("pastForm", "과거형");
        formNames.put("presentForm", "현재형");
        formNames.put("causativeForm", "사역형");
        formNames.put("passiveForm", "수동형");

        // 형용사 활용형
        formNames.put("mizenForm", "미연형");
        formNames.put("politeForm", "정중형");
        formNames.put("adverbialForm", "부사형");
        formNames.put("nounForm", "명사형");

        return formNames.getOrDefault(formType, formType);
    }

    /**
     * 예문에서 기본형을 빈칸으로 만드는 기존 예문 학습 기능
     */
    private String createBlankSentence(
            String sentence,
            String word,
            String reading) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return "例文がありません。";
        }

        String blankSentence = sentence;

        if (word != null && !word.trim().isEmpty()) {
            blankSentence = blankSentence.replace(word, "（　）");
        }

        if (reading != null
                && !reading.trim().isEmpty()
                && !blankSentence.contains("（　）")) {
            blankSentence = blankSentence.replace(reading, "（　）");
        }

        return blankSentence;
    }

    /**
     * 힌트 생성
     */
    private String getHint(String answer) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }

        if (answer.length() == 1) {
            return "힌트: " + answer;
        }

        return "힌트: " + answer.charAt(0) + "...";
    }

    /**
     * 정답 확인
     */
    public boolean checkAnswer(Long wordId, String answer, String formType) {
        logger.debug("정답 확인 - 단어 ID: {}, 활용형: {}, 답안: {}",
                wordId, formType, answer);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return false;
            }

            WordDto wordDto = new WordDto(word);

            if ("word".equals(formType) || "원형".equals(formType)) {
                boolean isCorrect = word.getWord().equals(answer)
                        || word.getReading().equals(answer);

                return isCorrect;
            }

            if (wordDto.getForms() != null
                    && wordDto.getForms().containsKey(formType)) {

                String correctAnswer = wordDto.getForms().get(formType);

                return correctAnswer != null
                        && correctAnswer.equals(answer);
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})",
                    formType, wordId);

            return false;

        } catch (Exception e) {
            logger.error("정답 확인 중 오류 발생 - 단어 ID: {}", wordId, e);
            return false;
        }
    }

    /**
     * 정답 반환
     */
    public String getCorrectAnswer(Long wordId, String formType) {
        logger.debug("정답 조회 - 단어 ID: {}, 활용형: {}", wordId, formType);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return "";
            }

            WordDto wordDto = new WordDto(word);

            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord();
            }

            if (wordDto.getForms() != null
                    && wordDto.getForms().containsKey(formType)) {
                return wordDto.getForms().get(formType);
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})",
                    formType, wordId);

            return "";

        } catch (Exception e) {
            logger.error("정답 조회 중 오류 발생 - 단어 ID: {}", wordId, e);
            return "";
        }
    }

    /**
     * 특정 단어의 활용형 목록 조회
     */
    public List<String> getAvailableForms(Long wordId) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return Collections.emptyList();
            }

            WordDto wordDto = new WordDto(word);

            if (wordDto.getForms() == null) {
                return Collections.emptyList();
            }

            return wordDto.getForms().entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() != null
                            && !entry.getValue().trim().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("활용형 목록 조회 중 오류 발생 - 단어 ID: {}", wordId, e);
            return Collections.emptyList();
        }
    }
}
