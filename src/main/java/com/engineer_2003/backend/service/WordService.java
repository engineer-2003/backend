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
        // 기존 로직 유지 (WordDto 생성 시 LinkedHashMap으로 순서 보장됨)
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

            Collections.shuffle(wordDtos); // 단어 순서는 섞지만, 내부 활용형 순서는 WordDto에서 보장됨
            return wordDtos;
        } catch (Exception e) {
            throw new RuntimeException("단어 목록 조회 실패", e);
        }
    }

    public WordDto getWordById(Long id) {
        // 기존 로직 유지
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

            // 수정: 한자와 읽기 모두 빈칸으로 처리
            String blankSentence = createBlankSentence(word.getExampleSentence(), word.getWord(), word.getReading());
            problem.put("blankSentence", blankSentence);

            // 정답 목록에 원형과 읽기 모두 포함
            problem.put("correctAnswers", Arrays.asList(word.getWord(), word.getReading()));
            problem.put("hint", getHint(word.getReading()));

            return problem;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> generateFormProblem(Long wordId) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return null;

            WordDto wordDto = new WordDto(word);
            List<String> availableForms = getAvailableForms(wordId);
            if (availableForms.isEmpty()) return null;

            // 랜덤하게 문제 출제 (학습 페이지에서는 프론트가 순서대로 요청하므로 이 메서드는 단일 조회용)
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

            return problem;
        } catch (Exception e) {
            return null;
        }
    }

    // 수정: 한자와 읽기 모두 확실하게 빈칸으로 치환
    private String createBlankSentence(String sentence, String word, String reading) {
        if (sentence == null || sentence.trim().isEmpty()) return "例文がありません。";

        String blankSentence = sentence;
        // 한자 단어 치환
        if (word != null && !word.trim().isEmpty()) {
            blankSentence = blankSentence.replace(word, "（　）");
        }
        // 읽기(히라가나) 치환 (예: 단어가 한자로 되어있지 않고 히라가나로만 예문에 쓰인 경우 대비)
        if (reading != null && !reading.trim().isEmpty()) {
            blankSentence = blankSentence.replace(reading, "（　）");
        }
        return blankSentence;
    }

    private String getHint(String answer) {
        if (answer == null || answer.isEmpty()) return "";
        return answer.length() == 1 ? "힌트: " + answer : "힌트: " + answer.charAt(0) + "...";
    }

    // 수정: 히라가나/한자 모두 정답 인정 로직 강화
    public boolean checkAnswer(Long wordId, String answer, String formType) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) return false;

            WordDto wordDto = new WordDto(word);
            String trimmedAnswer = answer.trim();

            // 1. 원형(예문) 학습 확인
            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord().equals(trimmedAnswer) || word.getReading().equals(trimmedAnswer);
            }

            // 2. 활용형 학습 확인
            if (wordDto.getForms().containsKey(formType)) {
                String correctAnswer = wordDto.getForms().get(formType);
                if (correctAnswer == null) return false;

                // 2-1. 정확히 일치하는 경우 (한자 정답)
                if (correctAnswer.equals(trimmedAnswer)) return true;

                // 2-2. 히라가나 변환 시도 (휴리스틱)
                // DB 정답이 한자(예: 食べます)인데 사용자 입력이 히라가나(예: たべます)인 경우 처리
                if (word.getWord().length() > 0 && word.getReading().length() > 0) {
                    char firstKanji = word.getWord().charAt(0);
                    char firstKana = word.getReading().charAt(0);

                    // 정답이 해당 단어의 첫 한자로 시작하고, 그 한자가 실제 한자 범위인 경우
                    if (correctAnswer.charAt(0) == firstKanji && isKanji(firstKanji)) {
                        // 정답 문자열의 첫 한자를 단어의 첫 읽기(히라가나)로 치환하여 비교
                        // 예: "食べます" -> "た" + "べます" = "たべます"
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
                return word.getWord(); // 대표 정답은 원형으로
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

            // WordDto 생성 시 LinkedHashMap을 사용했으므로 여기서 keySet 순서가 보장됨
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
        formNames.put("mizenForm", "미연형");
        formNames.put("politeForm", "정중형");
        formNames.put("adverbialForm", "부사형");
        formNames.put("nounForm", "명사형");
        return formNames.getOrDefault(formType, formType);
    }
}