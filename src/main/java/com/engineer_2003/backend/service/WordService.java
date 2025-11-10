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
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(levelName, partOfSpeech);
            logger.debug("DB에서 조회된 단어 수: {}", words.size());

            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try {
                            return new WordDto(word);
                        } catch (Exception e) {
                            logger.warn("WordDto 변환 실패 - 단어 ID: {}", word.getId(), e);
                            return null;
                        }
                    })
                    .filter(dto -> dto != null) // null인 항목 제거
                    // ------ 이 부분 수정 --------
                    // 품사가 명사가 아니고, 활용형이 있어야 하는 경우에만 활용형 필터를 적용
                    .filter(dto -> {
                        if (("동사".equals(partOfSpeech) || "형용사".equals(partOfSpeech))) {
                            // 동사 또는 형용사일 경우 활용형이 비어있으면 필터링
                            return dto.getForms() != null && !dto.getForms().isEmpty();
                        } else {
                            // 그 외 품사 (명사 등)는 활용형 필터 적용 없이 통과
                            return true;
                        }
                    })
                    // -----------------------------
                    .collect(Collectors.toList());

            logger.debug("변환된 WordDto 수: {}", wordDtos.size());

            // 랜덤하게 섞기
            Collections.shuffle(wordDtos);

            logger.info("단어 목록 조회 완료 - 레벨: {}, 품사: {}, 반환 개수: {}",
                    levelName, partOfSpeech, wordDtos.size());

            return wordDtos;

        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생 - 레벨: {}, 품사: {}", levelName, partOfSpeech, e);
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

            WordDto wordDto = new WordDto(word);
            logger.debug("단어 조회 성공 - ID: {}, 단어: {}", id, word.getWord());

            return wordDto;

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
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return null;
            }

            // 1. 예문이 없으면 문제 생성하지 않음
            if (word.getExampleSentence() == null || word.getExampleSentence().trim().isEmpty()) {
                logger.warn("예문이 없어 문제 생성 불가 - 단어 ID: {}", wordId);
                return null;
            }

            Map<String, Object> problem = new HashMap<>();

            // 예문에서 빈칸 만들기 (단어/읽기 모두 대체 시도)
            String blankSentence = createBlankSentence(word.getExampleSentence(), word.getWord(), word.getReading());

            // 2. 빈칸이 만들어지지 않았다면 문제 생성하지 않음
            if (!blankSentence.contains("（　）")) {
                logger.warn("예문에서 빈칸을 만들 수 없어 문제 생성 불가 - 단어 ID: {}", wordId);
                return null;
            }

            // 기본 정보
            problem.put("id", word.getId());
            problem.put("meaning", word.getMeaning());
            problem.put("exampleTranslation", word.getExampleTranslation());
            problem.put("level", word.getLevelName());
            problem.put("part", word.getPartOfSpeech());
            problem.put("blankSentence", blankSentence); // 수정된 blankSentence 추가

            // 정답은 원형
            problem.put("correctAnswers", Arrays.asList(word.getWord(), word.getReading()));
            problem.put("hint", getHint(word.getReading()));

            logger.debug("예문 문제 생성 완료 - 단어 ID: {}", wordId);
            return problem;

        } catch (Exception e) {
            logger.error("예문 문제 생성 중 오류 발생 - 단어 ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 활용형 문제 생성
     */
    public Map<String, Object> generateFormProblem(Long wordId) {
        logger.debug("활용형 문제 생성 - 단어 ID: {}", wordId);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return null;
            }

            WordDto wordDto = new WordDto(word);

            // 동사나 형용사가 아니면 활용형 문제를 생성할 수 없음
            if (!"동사".equals(word.getPartOfSpeech()) && !"형용사".equals(word.getPartOfSpeech())) {
                logger.warn("활용형이 없는 품사 - 단어 ID: {}, 품사: {}", wordId, word.getPartOfSpeech());
                return null;
            }

            // 사용 가능한 활용형 목록 가져오기
            List<String> availableForms = getAvailableForms(wordId);
            if (availableForms.isEmpty()) {
                logger.warn("사용 가능한 활용형이 없음 - 단어 ID: {}", wordId);
                return null;
            }

            // 랜덤하게 활용형 선택
            Random random = new Random();
            String selectedForm = availableForms.get(random.nextInt(availableForms.size()));
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

            logger.debug("활용형 문제 생성 완료 - 단어 ID: {}, 활용형: {}", wordId, selectedForm);
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
     * 예문에서 빈칸 만들기 (백엔드에서 처리)
     */
    private String createBlankSentence(String sentence, String word, String reading) {
        // generateSentenceProblem에서 null/empty 체크를 수행하므로, 여기서는 바로 로직 실행
        String blankSentence = sentence;

        // 한자 형태의 단어를 빈칸으로
        if (word != null && !word.trim().isEmpty()) {
            // 모든 일치 항목을 대체
            blankSentence = blankSentence.replace(word, "（　）");
        }

        // 히라가나 읽기도 빈칸으로 (앞선 한자 대체 여부와 상관없이 무조건 대체 시도)
        if (reading != null && !reading.trim().isEmpty()) {
            // 모든 일치 항목을 대체
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

    public boolean checkAnswer(Long wordId, String answer, String formType) {
        logger.debug("정답 확인 - 단어 ID: {}, 활용형: {}, 답안: {}", wordId, formType, answer);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return false;
            }

            WordDto wordDto = new WordDto(word);

            // 기본 단어 확인 (원형)
            if ("word".equals(formType) || "원형".equals(formType)) {
                boolean isCorrect = word.getWord().equals(answer) || word.getReading().equals(answer);
                logger.debug("원형 정답 확인 결과: {} (단어: {}, 읽기: {}, 답안: {})",
                        isCorrect, word.getWord(), word.getReading(), answer);
                return isCorrect;
            }

            // 활용형 확인
            if (wordDto.getForms().containsKey(formType)) {
                String correctAnswer = wordDto.getForms().get(formType);
                boolean isCorrect = correctAnswer != null && correctAnswer.equals(answer);

                logger.debug("활용형 정답 확인 결과: {} (정답: {}, 답안: {})",
                        isCorrect, correctAnswer, answer);
                return isCorrect;
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})", formType, wordId);
            return false;

        } catch (Exception e) {
            logger.error("정답 확인 중 오류 발생 - 단어 ID: {}", wordId, e);
            return false;
        }
    }

    /**
     * 정답을 반환하는 메서드 (힌트나 정답 표시용)
     */
    public String getCorrectAnswer(Long wordId, String formType) {
        logger.debug("정답 조회 - 단어 ID: {}, 활용형: {}", wordId, formType);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return "";
            }

            WordDto wordDto = new WordDto(word);

            // 기본 단어 반환
            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord();
            }

            // 활용형 반환
            if (wordDto.getForms().containsKey(formType)) {
                return wordDto.getForms().get(formType);
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})", formType, wordId);
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
            return wordDto.getForms().keySet().stream()
                    .filter(key -> wordDto.getForms().get(key) != null && !wordDto.getForms().get(key).isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("활용형 목록 조회 중 오류 발생 - 단어 ID: {}", wordId, e);
            return Collections.emptyList();
        }
    }
}