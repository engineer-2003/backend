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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        logger.debug("단어 조회 시작 - 레벨: {}, 품사: {}",
                levelName, partOfSpeech);

        try {
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(
                    levelName, partOfSpeech);

            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try {
                            return new WordDto(word);
                        } catch (Exception e) {
                            logger.warn("WordDto 변환 실패 - ID: {}",
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

            problem.put("correctAnswers",
                    Arrays.asList(word.getWord(), word.getReading()));

            problem.put("hint", getHint(word.getReading()));

            return problem;

        } catch (Exception e) {
            logger.error("예문 문제 생성 오류 - ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 예문에 실제 등장하는 활용형을 자동으로 찾아
     * 그중 하나를 무작위로 출제합니다.
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

            /*
             * 예문에 실제로 등장하는 활용형 검색
             */
            List<FormOccurrence> occurrences = findFormOccurrences(exampleSentence, forms);

            if (occurrences.isEmpty()) {

                logger.info(
                        "예문에 실제 활용형이 없음 - ID: {}, 단어: {}",
                        wordId, word.getWord());

                return null;
            }

            /*
             * 동일한 위치에 여러 형태가 매칭되는 경우
             * 더 긴 형태를 우선합니다.
             *
             * 서로 다른 위치에 등장하는 활용형은
             * 모두 무작위 선택 대상에 포함합니다.
             */
            int maxLength = occurrences.stream()
                    .mapToInt(FormOccurrence::getLength)
                    .max()
                    .orElse(0);

            List<FormOccurrence> candidates = occurrences.stream()
                    .filter(o -> o.getLength() == maxLength)
                    .collect(Collectors.toList());

            FormOccurrence selected = candidates.get(
                    random.nextInt(candidates.size()));

            String formType = selected.getFormType();
            String correctAnswer = selected.getMatchedText();

            /*
             * 실제로 매칭된 문자열 부분만 빈칸으로 변경
             */
            String blankSentence = exampleSentence.substring(0, selected.getStart())
                    + "（　）"
                    + exampleSentence.substring(selected.getEnd());

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

            problem.put("exampleTranslation",
                    word.getExampleTranslation());

            problem.put("hint", getHint(correctAnswer));

            logger.info(
                    "활용형 문제 생성 성공 - ID: {}, 단어: {}, 타입: {}, 정답: {}",
                    wordId, word.getWord(), formType, correctAnswer);

            return problem;

        } catch (Exception e) {
            logger.error("활용형 문제 생성 오류 - ID: {}", wordId, e);
            return null;
        }
    }

    /**
     * 예문에서 활용형이 등장하는 위치를 검색합니다.
     *
     * 괄호 표기 예시:
     * 明(か)します -> 明かします
     * 荒(ら)します -> 荒らします
     *
     * DB의 괄호는 표기상의 구분으로 취급하고
     * 괄호 안의 문자는 실제 예문에서 필수로 검색합니다.
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

            String regex = buildFormRegex(formValue);

            try {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(sentence);

                while (matcher.find()) {

                    String matchedText = matcher.group();

                    if (!isValidForm(matchedText)) {
                        continue;
                    }

                    results.add(new FormOccurrence(
                            formType,
                            formValue,
                            matchedText,
                            matcher.start(),
                            matcher.end()));
                }

            } catch (Exception e) {
                logger.warn("활용형 검색 실패 - 타입: {}, 형태: {}",
                        formType, formValue, e);
            }
        }

        /*
         * 같은 위치에 중복으로 매칭된 경우
         * 긴 문자열을 우선하여 정리합니다.
         */
        results.sort(
                Comparator.comparingInt(FormOccurrence::getLength)
                        .reversed());

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
     * DB의 괄호 표기를 검색 가능한 정규식으로 변환합니다.
     *
     * 明(か)します -> 明かします
     *
     * 괄호 자체는 검색하지 않고 내부 문자는 검색합니다.
     */
    private String buildFormRegex(String form) {

        StringBuilder regex = new StringBuilder();

        boolean insideParentheses = false;

        for (int i = 0; i < form.length(); i++) {

            char c = form.charAt(i);

            if (c == '(' || c == '（') {
                insideParentheses = true;
                continue;
            }

            if (c == ')' || c == '）') {
                insideParentheses = false;
                continue;
            }

            regex.append(Pattern.quote(String.valueOf(c)));
        }

        return regex.toString();
    }

    /**
     * 활용형 데이터 유효성 검사
     */
    private boolean isValidForm(String form) {

        if (form == null || form.trim().isEmpty()) {
            return false;
        }

        String value = form.trim();

        return !value.equals("null")
                && !value.equals("-")
                && !value.equals("NaN");
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

        return formNames.getOrDefault(formType, formType);
    }

    /**
     * 기존 기본형 예문 학습
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
    public boolean checkAnswer(
            Long wordId, String answer, String formType) {

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);

            if (word == null || answer == null) {
                return false;
            }

            if ("word".equals(formType) || "원형".equals(formType)) {

                return Objects.equals(word.getWord(), answer)
                        || Objects.equals(word.getReading(), answer);
            }

            WordDto wordDto = new WordDto(word);

            Map<String, String> forms = wordDto.getForms();

            if (forms == null || !forms.containsKey(formType)) {
                return false;
            }

            String correctAnswer = forms.get(formType);

            return correctAnswer != null
                    && correctAnswer.equals(answer);

        } catch (Exception e) {
            logger.error("정답 확인 오류 - ID: {}", wordId, e);
            return false;
        }
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

            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord();
            }

            WordDto wordDto = new WordDto(word);

            Map<String, String> forms = wordDto.getForms();

            if (forms == null) {
                return "";
            }

            return forms.getOrDefault(formType, "");

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
