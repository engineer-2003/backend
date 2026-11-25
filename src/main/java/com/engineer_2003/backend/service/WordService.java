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

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WordService {

    private static final Logger logger = LoggerFactory.getLogger(WordService.class);

    @Autowired
    private WordRepository wordRepository;

    private final Random random = new Random();

    /**
     * 급수 및 품사별 단어 조회
     */
    public List<WordDto> getWordsByLevelAndPart(
            String levelName, String partOfSpeech) {

        logger.info(
                "단어 조회 시작 - 레벨: {}, 품사: {}",
                levelName, partOfSpeech);

        try {
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(
                    levelName, partOfSpeech);

            logger.info("Repository 반환 단어 수: {}", words.size());

            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try {
                            return new WordDto(word);
                        } catch (Exception e) {
                            logger.warn(
                                    "WordDto 변환 실패 - ID: {}",
                                    word.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(dto -> {
                        if ("동사".equals(partOfSpeech)
                                || "형용사".equals(partOfSpeech)) {

                            return dto.getForms() != null
                                    && dto.getForms().values().stream()
                                            .anyMatch(this::isValidForm);
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            Collections.shuffle(wordDtos);

            logger.info(
                    "단어 목록 조회 완료 - 레벨: {}, 품사: {}, 개수: {}",
                    levelName, partOfSpeech, wordDtos.size());

            return wordDtos;

        } catch (Exception e) {
            logger.error("단어 목록 조회 오류", e);
            throw new RuntimeException("단어 목록 조회 실패", e);
        }
    }

    /**
     * 단어 상세 조회
     */
    public WordDto getWordById(Long id) {

        try {
            Word word = wordRepository.findWordWithFormsById(id);

            if (word == null) {
                return null;
            }

            return new WordDto(word);

        } catch (Exception e) {
            logger.error("단어 조회 오류 - ID: {}", id, e);
            throw new RuntimeException("단어 조회 실패", e);
        }
    }

    /**
     * 예문 문제 생성
     */
    public Map<String, Object> generateSentenceProblem(Long wordId) {

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return null;
            }

            Map<String, Object> problem = new HashMap<>();

            problem.put("id", word.getId());
            problem.put("meaning", word.getMeaning());
            problem.put("exampleTranslation",
                    word.getExampleTranslation());

            problem.put("level", word.getLevelName());
            problem.put("part", word.getPartOfSpeech());

            String blankSentence = createBlankSentence(
                    word.getExampleSentence(),
                    word.getWord(),
                    word.getReading());

            problem.put("blankSentence", blankSentence);

            problem.put(
                    "correctAnswers",
                    Arrays.asList(word.getWord(), word.getReading()));

            problem.put("hint", getHint(word.getReading()));

            return problem;

        } catch (Exception e) {
            logger.error("예문 문제 생성 오류 - ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 활용형 문제 생성
     *
     * 1. 예문에 활용형이 있으면 해당 활용형을 빈칸으로 만듭니다.
     * 2. 예문에 활용형이 없으면 기본형을 빈칸으로 만듭니다.
     * 3. 정답은 DB에 저장된 활용형에서 가져옵니다.
     */
    public Map<String, Object> generateFormProblem(Long wordId) {

        logger.debug("활용형 문제 생성 시작 - ID: {}", wordId);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return null;
            }

            String part = word.getPartOfSpeech();

            if (!"동사".equals(part) && !"형용사".equals(part)) {
                return null;
            }

            String exampleSentence = word.getExampleSentence();

            if (exampleSentence == null
                    || exampleSentence.trim().isEmpty()) {

                logger.info("예문 없음 - ID: {}", wordId);
                return null;
            }

            WordDto wordDto = new WordDto(word);
            Map<String, String> forms = wordDto.getForms();

            if (forms == null || forms.isEmpty()) {
                logger.info("활용형 데이터 없음 - ID: {}", wordId);
                return null;
            }

            // 유효한 활용형만 추출
            Map<String, String> validForms = forms.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null
                            && isValidForm(entry.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new));

            if (validForms.isEmpty()) {
                logger.info("유효한 활용형 없음 - ID: {}", wordId);
                return null;
            }

            String selectedFormType;
            String correctAnswer;
            String blankSentence;
            String matchedText;

            // 1단계: 예문에서 실제 활용형 검색
            List<FormOccurrence> occurrences = findFormOccurrences(exampleSentence, validForms);

            if (!occurrences.isEmpty()) {

                int maxLength = occurrences.stream()
                        .mapToInt(FormOccurrence::getLength)
                        .max()
                        .orElse(0);

                List<FormOccurrence> candidates = occurrences.stream()
                        .filter(o -> o.getLength() == maxLength)
                        .collect(Collectors.toList());

                FormOccurrence selected = candidates.get(
                        random.nextInt(candidates.size()));

                selectedFormType = selected.getFormType();
                correctAnswer = selected.getMatchedText();
                matchedText = selected.getMatchedText();

                blankSentence = exampleSentence.substring(0, selected.getStart())
                        + "（　）"
                        + exampleSentence.substring(selected.getEnd());

                logger.debug(
                        "예문에서 활용형 발견 - ID: {}, 타입: {}",
                        wordId, selectedFormType);

            } else {

                // 2단계: 활용형이 없으면 기본형을 찾아 빈칸 처리
                logger.info(
                        "예문에 활용형 없음. 기본형 fallback 실행 - ID: {}, 단어: {}",
                        wordId, word.getWord());

                List<String> availableFormTypes = new ArrayList<>(validForms.keySet());

                selectedFormType = availableFormTypes.get(
                        random.nextInt(availableFormTypes.size()));

                correctAnswer = validForms.get(selectedFormType);

                matchedText = findBaseWordInSentence(
                        exampleSentence,
                        word.getWord(),
                        word.getReading());

                if (matchedText == null) {
                    logger.info(
                            "예문에서 기본형도 찾지 못함 - ID: {}, 예문: {}",
                            wordId, exampleSentence);
                    return null;
                }

                blankSentence = replaceFirstExact(
                        exampleSentence,
                        matchedText,
                        "（　）");

                if (blankSentence.equals(exampleSentence)) {
                    return null;
                }
            }

            Map<String, Object> problem = new HashMap<>();

            problem.put("id", word.getId());
            problem.put("word", word.getWord());
            problem.put("reading", word.getReading());
            problem.put("meaning", word.getMeaning());

            problem.put("part", word.getPartOfSpeech());
            problem.put("level", word.getLevelName());

            problem.put("formType", selectedFormType);
            problem.put("formName",
                    getFormDisplayName(selectedFormType));

            problem.put("correctAnswer", correctAnswer);

            problem.put("blankSentence", blankSentence);
            problem.put("exampleSentence", exampleSentence);

            problem.put(
                    "exampleTranslation",
                    word.getExampleTranslation());

            problem.put("hint", getHint(correctAnswer));

            // fallback 여부를 프론트에서도 확인할 수 있도록 전달
            problem.put(
                    "fallbackUsed",
                    occurrences.isEmpty());

            // 예문에서 실제로 빈칸 처리한 문자열
            problem.put("matchedText", matchedText);

            logger.info(
                    "활용형 문제 생성 성공 - ID: {}, 단어: {}, 타입: {}, 정답: {}, fallback: {}",
                    wordId,
                    word.getWord(),
                    selectedFormType,
                    correctAnswer,
                    occurrences.isEmpty());

            return problem;

        } catch (Exception e) {
            logger.error("활용형 문제 생성 오류 - ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 예문에서 기본형 검색
     */
    private String findBaseWordInSentence(
            String sentence,
            String word,
            String reading) {

        if (sentence == null) {
            return null;
        }

        // 먼저 DB의 word 필드로 검색
        if (word != null && !word.trim().isEmpty()) {

            String found = findExactText(sentence, word);

            if (found != null) {
                return found;
            }

            // 괄호 표기가 있는 경우도 비교
            found = findNormalizedText(sentence, word);

            if (found != null) {
                return found;
            }
        }

        // word로 찾지 못하면 reading으로 검색
        if (reading != null && !reading.trim().isEmpty()) {

            String found = findExactText(sentence, reading);

            if (found != null) {
                return found;
            }

            found = findNormalizedText(sentence, reading);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * 원문에서 정확한 문자열 검색
     */
    private String findExactText(String sentence, String target) {

        if (sentence == null || target == null || target.isEmpty()) {
            return null;
        }

        return sentence.contains(target) ? target : null;
    }

    /**
     * 괄호 등을 무시한 정규화 검색
     * 검색된 문자열은 원문에서 실제 위치를 찾아 반환합니다.
     */
    private String findNormalizedText(String sentence, String target) {

        String normalizedTarget = normalizeText(target);

        if (normalizedTarget.isEmpty()) {
            return null;
        }

        for (int start = 0; start < sentence.length(); start++) {

            for (int end = start + 1; end <= sentence.length(); end++) {

                String candidate = sentence.substring(start, end);

                if (normalizeText(candidate).equals(normalizedTarget)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * 문자열 첫 번째 일치 항목 치환
     */
    private String replaceFirstExact(
            String sentence,
            String target,
            String replacement) {

        if (sentence == null || target == null || target.isEmpty()) {
            return sentence;
        }

        int index = sentence.indexOf(target);

        if (index < 0) {
            return sentence;
        }

        return sentence.substring(0, index)
                + replacement
                + sentence.substring(index + target.length());
    }

    /**
     * 예문과 DB 활용형 비교
     */
    private List<FormOccurrence> findFormOccurrences(
            String sentence,
            Map<String, String> forms) {

        List<FormOccurrence> results = new ArrayList<>();

        if (sentence == null || forms == null) {
            return results;
        }

        for (Map.Entry<String, String> entry : forms.entrySet()) {

            String formType = entry.getKey();
            String formValue = entry.getValue();

            if (formType == null || !isValidForm(formValue)) {
                continue;
            }

            String normalizedForm = normalizeText(formValue);

            if (normalizedForm.isEmpty()) {
                continue;
            }

            int index = sentence.indexOf(formValue);

            while (index >= 0) {

                results.add(new FormOccurrence(
                        formType,
                        formValue,
                        sentence.substring(
                                index,
                                index + formValue.length()),
                        index,
                        index + formValue.length()));

                index = sentence.indexOf(
                        formValue,
                        index + 1);
            }
        }

        // 긴 활용형 우선
        results.sort(
                Comparator.comparingInt(
                        FormOccurrence::getLength).reversed());

        // 겹치는 매칭 제거
        List<FormOccurrence> filtered = new ArrayList<>();

        for (FormOccurrence current : results) {

            boolean overlaps = filtered.stream().anyMatch(existing -> current.getStart() < existing.getEnd()
                    && current.getEnd() > existing.getStart());

            if (!overlaps) {
                filtered.add(current);
            }
        }

        return filtered;
    }

    /**
     * 비교용 정규화
     */
    private String normalizeText(String text) {

        if (text == null) {
            return "";
        }

        return Normalizer.normalize(
                text,
                Normalizer.Form.NFKC)
                .replaceAll("[()（）]", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    /**
     * 활용형 데이터 유효성 검사
     */
    private boolean isValidForm(String form) {

        if (form == null || form.trim().isEmpty()) {
            return false;
        }

        String value = form.trim();

        return !value.equalsIgnoreCase("null")
                && !value.equals("-")
                && !value.equalsIgnoreCase("NaN");
    }

    /**
     * 활용형 검색 결과 저장용 클래스
     */
    private static class FormOccurrence {

        private final String formType;
        private final String originalForm;
        private final String matchedText;
        private final int start;
        private final int end;

        public FormOccurrence(
                String formType,
                String originalForm,
                String matchedText,
                int start,
                int end) {

            this.formType = formType;
            this.originalForm = originalForm;
            this.matchedText = matchedText;
            this.start = start;
            this.end = end;
        }

        public String getFormType() {
            return formType;
        }

        public String getOriginalForm() {
            return originalForm;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getLength() {
            return end - start;
        }
    }

    /**
     * 활용형 표시명
     */
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

        // 한국어 키
        formNames.put("ます형", "ます형");
        formNames.put("부정형", "부정형");
        formNames.put("의지형", "의지형");
        formNames.put("조건형", "조건형");
        formNames.put("가정형", "가정형");
        formNames.put("명령형", "명령형");
        formNames.put("가능형", "가능형");
        formNames.put("て형", "て형");
        formNames.put("た형", "た형");
        formNames.put("과거형", "과거형");
        formNames.put("미연형", "미연형");
        formNames.put("정중형", "정중형");

        return formNames.getOrDefault(formType, formType);
    }

    /**
     * 기본형 예문 학습
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

            blankSentence = blankSentence.replace(
                    reading, "（　）");
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
    public boolean checkAnswer(
            Long wordId,
            String answer,
            String formType) {

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null || answer == null) {
                return false;
            }

            if ("word".equals(formType)
                    || "원형".equals(formType)) {

                return Objects.equals(word.getWord(), answer)
                        || Objects.equals(word.getReading(), answer);
            }

            WordDto wordDto = new WordDto(word);
            Map<String, String> forms = wordDto.getForms();

            if (forms == null) {
                return false;
            }

            String correctAnswer = forms.get(formType);

            if (!isValidForm(correctAnswer)) {
                return false;
            }

            String normalizedCorrect = normalizeText(correctAnswer);
            String normalizedAnswer = normalizeText(answer);

            return normalizedCorrect.equals(normalizedAnswer)
                    || convertKatakanaToHiragana(normalizedCorrect)
                            .equals(convertKatakanaToHiragana(
                                    normalizedAnswer));

        } catch (Exception e) {
            logger.error("정답 확인 오류 - ID: {}", wordId, e);
            return false;
        }
    }

    /**
     * 가타카나를 히라가나로 변환
     */
    private String convertKatakanaToHiragana(String text) {

        if (text == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (char ch : text.toCharArray()) {

            if (ch >= '\u30A1' && ch <= '\u30F6') {
                result.append((char) (ch - 0x60));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    /**
     * 정답 반환
     */
    public String getCorrectAnswer(Long wordId, String formType) {

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return "";
            }

            if ("word".equals(formType)
                    || "원형".equals(formType)) {
                return word.getWord();
            }

            WordDto wordDto = new WordDto(word);
            Map<String, String> forms = wordDto.getForms();

            if (forms == null) {
                return "";
            }

            String answer = forms.get(formType);

            return isValidForm(answer) ? answer : "";

        } catch (Exception e) {
            logger.error("정답 조회 오류 - ID: {}", wordId, e);
            return "";
        }
    }

    /**
     * 특정 단어의 사용 가능한 활용형 목록
     */
    public List<String> getAvailableForms(Long wordId) {

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null) {
                return Collections.emptyList();
            }

            WordDto wordDto = new WordDto(word);
            Map<String, String> forms = wordDto.getForms();

            if (forms == null) {
                return Collections.emptyList();
            }

            return forms.entrySet().stream()
                    .filter(entry -> isValidForm(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("활용형 목록 조회 오류 - ID: {}", wordId, e);
            return Collections.emptyList();
        }
    }
}
