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

            if (!"동사".equals(word.getPartOfSpeech()) && !"형용사".equals(word.getPartOfSpeech())) {
                return null;
            }

            String selectedForm;
            if (formType != null && !formType.trim().isEmpty() && wordDto.getForms().containsKey(formType)) {
                selectedForm = formType;
            } else {
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
            // 사용자 입력값 공백 제거 및 정규화
            String trimmedAnswer = answer.trim().replace(" ", "");

            // 1. 원형 확인
            if ("word".equals(formType) || "원형".equals(formType)) {
                // 원형 비교 시에도 읽기에서 모든 하이픈 제거 후 비교
                String cleanReading = word.getReading().replaceAll("[\\-‐−–—]", "");
                return word.getWord().equals(trimmedAnswer) || cleanReading.equals(trimmedAnswer);
            }

            // 2. 활용형 확인
            if (wordDto.getForms().containsKey(formType)) {
                String correctAnswer = wordDto.getForms().get(formType);
                if (correctAnswer == null) return false;

                // 2-1. 정확한 일치 (한자 포함 정답)
                if (correctAnswer.equals(trimmedAnswer)) return true;

                // 2-2. 히라가나 변환 확인 (개선된 로직)
                if (word.getWord().length() > 0 && word.getReading().length() > 0) {
                    String kanjiWord = word.getWord();      // 예: 赤い

                    // 읽기 데이터에서 하이픈(-, ‐) 제거
                    // ASCII Hyphen, Unicode Hyphen, Minus Sign, En Dash, Em Dash 포함
                    String kanaReading = word.getReading().replaceAll("[\\-‐−–—]", "");

                    // 공통 접미사(오쿠리가나) 길이 계산
                    int suffixLength = getCommonSuffixLength(kanjiWord, kanaReading);

                    // 어간 추출 (접미사 제외)
                    if (kanjiWord.length() > suffixLength && kanaReading.length() > suffixLength) {
                        String kanjiStem = kanjiWord.substring(0, kanjiWord.length() - suffixLength);
                        String kanaStem = kanaReading.substring(0, kanaReading.length() - suffixLength);

                        // 정답 문자열에 한자 어간이 포함되어 있다면, 히라가나 어간으로 치환하여 비교
                        if (correctAnswer.contains(kanjiStem)) {
                            String pseudoHiraganaAnswer = correctAnswer.replace(kanjiStem, kanaStem);

                            // 디버깅 로그 (문제가 지속될 경우 서버 로그 확인용)
                            logger.debug("히라가나 변환 체크 - 정답: {}, 변환된 답: {}, 사용자 입력: {}",
                                    correctAnswer, pseudoHiraganaAnswer, trimmedAnswer);

                            if (pseudoHiraganaAnswer.equals(trimmedAnswer)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("정답 확인 중 오류 발생", e);
            return false;
        }
    }

    // 공통 접미사(오쿠리가나) 길이 계산 헬퍼 메서드
    private int getCommonSuffixLength(String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        int minLen = Math.min(l1, l2);
        int suffixLen = 0;

        for (int i = 1; i <= minLen; i++) {
            if (s1.charAt(l1 - i) != s2.charAt(l2 - i)) {
                break;
            }
            suffixLen++;
        }
        return suffixLen;
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

        formNames.put("masuForm", "ます형");
        formNames.put("negativeForm", "부정형");
        formNames.put("volitionalForm", "청유형");
        formNames.put("conditionalForm", "가정형");
        formNames.put("imperativeForm", "명령형");
        formNames.put("potentialForm", "가능형");

        formNames.put("mizenForm", "미연형");
        formNames.put("teForm", "て형");
        formNames.put("pastForm", "과거형");
        formNames.put("politeForm", "정중형");

        return formNames.getOrDefault(formType, formType);
    }
}