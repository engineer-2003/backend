// StudyController.java
package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.StudyRecordDto;
import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.StudySession;
import com.engineer_2003.backend.entity.WordStudyResult;
import com.engineer_2003.backend.service.StudyRecordService;
import com.engineer_2003.backend.service.WordService;
import com.engineer_2003.backend.security.AuthUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/study")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyController {

    private static final Logger logger = LoggerFactory.getLogger(StudyController.class);

    @Autowired
    private WordService wordService;

    @Autowired
    private StudyRecordService studyRecordService;

    @Autowired
    private AuthUtils authUtils;

    /**
     * 전체 학습 통계 조회
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getOverviewStats(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("전체 학습 통계 조회 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            Map<String, Object> stats = studyRecordService.getOverviewStats(userId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("전체 학습 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 급수별 학습 통계 조회
     */
    @GetMapping("/stats/level")
    public ResponseEntity<List<Map<String, Object>>> getLevelStats(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("급수별 학습 통계 조회 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<Map<String, Object>> stats = studyRecordService.getLevelStats(userId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("급수별 학습 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 품사별 학습 통계 조회
     */
    @GetMapping("/stats/part")
    public ResponseEntity<List<Map<String, Object>>> getPartStats(
            @RequestParam String level,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("품사별 학습 통계 조회 요청 - 레벨: {}", level);

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<Map<String, Object>> stats = studyRecordService.getPartStats(userId, level);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("품사별 학습 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 급수 및 품사별 단어 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<WordDto>> getWords(
            @RequestParam String level,
            @RequestParam String part) {

        logger.info("단어 목록 요청 - 레벨: {}, 품사: {}", level, part);

        try {
            if (level == null || level.trim().isEmpty()
                    || part == null || part.trim().isEmpty()) {

                return ResponseEntity.badRequest().build();
            }

            List<WordDto> words = wordService.getWordsByLevelAndPart(
                    level.trim(),
                    part.trim());

            return ResponseEntity.ok(
                    words != null ? words : Collections.emptyList());

        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 단어 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<WordDto> getWord(
            @PathVariable Long id) {

        logger.info("단어 조회 요청 - ID: {}", id);

        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            WordDto word = wordService.getWordById(id);

            if (word == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(word);

        } catch (Exception e) {
            logger.error("단어 조회 중 오류 발생 - ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 예문 문제 생성
     */
    @GetMapping("/{id}/sentence-problem")
    public ResponseEntity<Map<String, Object>> generateSentenceProblem(
            @PathVariable Long id) {

        logger.info("예문 문제 생성 요청 - ID: {}", id);

        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> problem = wordService.generateSentenceProblem(id);

            if (problem == null || problem.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "문제를 생성할 수 없습니다.");

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error);
            }

            return ResponseEntity.ok(problem);

        } catch (Exception e) {
            logger.error("예문 문제 생성 오류 - ID: {}", id, e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "문제 생성 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 기존 단어 ID 기반 활용형 문제 생성
     */
    @GetMapping("/{id}/form-problem")
    public ResponseEntity<Map<String, Object>> generateFormProblem(
            @PathVariable Long id,
            @RequestParam(required = false) String formType) {

        logger.info("활용형 문제 생성 요청 - ID: {}, 타입: {}",
                id, formType);

        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> problem = wordService.generateFormProblem(id);

            if (problem == null || problem.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("available", false);
                error.put("error", "출제할 수 있는 활용형 예문이 없습니다.");

                return ResponseEntity.ok(error);
            }

            problem.put("available", true);

            return ResponseEntity.ok(problem);

        } catch (Exception e) {
            logger.error("활용형 문제 생성 오류 - ID: {}", id, e);

            Map<String, Object> error = new HashMap<>();
            error.put("available", false);
            error.put("error", "활용형 문제 생성 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 무작위 활용형 문제 자동 출제
     *
     * 1. 급수와 품사에 맞는 단어 조회
     * 2. 단어 목록 무작위 순회
     * 3. 예문에 실제 활용형이 있는 단어 탐색
     * 4. 출제 가능한 단어 발견 시 문제 반환
     * 5. 출제할 수 없는 단어는 건너뜀
     */
    @GetMapping("/form-problem/random")
    public ResponseEntity<Map<String, Object>> generateRandomFormProblem(
            @RequestParam String level,
            @RequestParam String part) {

        logger.info("무작위 활용형 문제 요청 - 레벨: {}, 품사: {}",
                level, part);

        try {
            if (level == null || level.trim().isEmpty()
                    || part == null || part.trim().isEmpty()) {

                return ResponseEntity.badRequest().build();
            }

            String normalizedPart = part.trim();

            if (!"동사".equals(normalizedPart)
                    && !"형용사".equals(normalizedPart)) {

                Map<String, Object> error = new HashMap<>();
                error.put("available", false);
                error.put("error", "동사 또는 형용사만 학습할 수 있습니다.");

                return ResponseEntity.badRequest().body(error);
            }

            List<WordDto> words = wordService.getWordsByLevelAndPart(
                    level.trim(),
                    normalizedPart);

            if (words == null || words.isEmpty()) {

                Map<String, Object> response = new HashMap<>();
                response.put("available", false);
                response.put("error", "학습 가능한 단어가 없습니다.");

                return ResponseEntity.ok(response);
            }

            // 단어 목록을 다시 무작위로 섞음
            List<WordDto> shuffledWords = new ArrayList<>(words);

            Collections.shuffle(shuffledWords);

            int checkedCount = 0;

            for (WordDto word : shuffledWords) {

                if (word == null || word.getId() == null) {
                    continue;
                }

                checkedCount++;

                Map<String, Object> problem = wordService.generateFormProblem(word.getId());

                /*
                 * 예문에 실제 활용형이 존재하는 단어만
                 * 출제 대상으로 선택
                 */
                if (problem != null && !problem.isEmpty()
                        && problem.get("correctAnswer") != null
                        && problem.get("formType") != null) {

                    problem.put("available", true);

                    logger.info(
                            "자동 활용형 출제 성공 - ID: {}, 단어: {}, 타입: {}",
                            word.getId(),
                            word.getWord(),
                            problem.get("formType"));

                    return ResponseEntity.ok(problem);
                }

                logger.debug(
                        "출제 불가 단어 건너뛰기 - ID: {}, 단어: {}",
                        word.getId(),
                        word.getWord());
            }

            Map<String, Object> response = new HashMap<>();

            response.put("available", false);
            response.put("checkedCount", checkedCount);
            response.put("error", "출제할 수 있는 활용형 예문이 없습니다.");

            logger.info(
                    "활용형 자동 출제 불가 - 검사 단어 수: {}",
                    checkedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            logger.error("무작위 활용형 문제 생성 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("available", false);
            error.put("error", "활용형 문제 생성 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 정답 확인 및 학습 결과 저장
     */
    @PostMapping("/{id}/check")
    public ResponseEntity<Map<String, Object>> checkAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("정답 확인 요청 - 단어 ID: {}", id);

        try {
            if (id == null || id <= 0) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "잘못된 단어 ID입니다.");

                return ResponseEntity.badRequest().body(error);
            }

            if (request == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "답안을 입력해주세요.");

                return ResponseEntity.badRequest().body(error);
            }

            String answer = request.get("answer");
            String formType = request.get("formType");
            String studyType = request.get("studyType");
            String sessionId = request.get("sessionId");

            if (answer == null || answer.trim().isEmpty()) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "답안을 입력해주세요.");

                return ResponseEntity.badRequest().body(error);
            }

            WordDto word = wordService.getWordById(id);

            if (word == null) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "단어를 찾을 수 없습니다.");

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error);
            }

            String trimmedAnswer = answer.trim();

            Map<String, String> forms = word.getForms();

            boolean isCorrect;
            boolean isConjugation = false;
            String correctAnswer;
            String normalizedFormType = formType == null ? null : formType.trim();

            /*
             * 활용형 정답 확인
             */
            if (normalizedFormType != null
                    && !normalizedFormType.isEmpty()
                    && forms != null
                    && forms.containsKey(normalizedFormType)
                    && isValidForm(forms.get(normalizedFormType))) {

                isConjugation = true;

                String originalForm = forms.get(normalizedFormType);

                correctAnswer = normalizeForm(originalForm);

                String normalizedAnswer = normalizeForm(trimmedAnswer);

                isCorrect = Objects.equals(
                        correctAnswer,
                        normalizedAnswer);

                formType = normalizedFormType;

                logger.info(
                        "활용형 정답 확인 - ID: {}, 타입: {}, 정답: {}, 입력: {}, 결과: {}",
                        id,
                        formType,
                        correctAnswer,
                        trimmedAnswer,
                        isCorrect);

            } else {

                /*
                 * 기본 단어 정답 확인
                 */
                correctAnswer = word.getWord();

                String reading = word.getReading();

                isCorrect = Objects.equals(
                        correctAnswer,
                        trimmedAnswer)
                        || (reading != null
                                && reading.equals(trimmedAnswer));

                formType = null;

                logger.info(
                        "기본 단어 정답 확인 - ID: {}, 입력: {}, 결과: {}",
                        id,
                        trimmedAnswer,
                        isCorrect);
            }

            /*
             * 학습 세션 카운트 업데이트
             */
            if (sessionId != null && !sessionId.trim().isEmpty()) {

                try {
                    Long sessId = Long.valueOf(sessionId);

                    studyRecordService.updateSessionCount(
                            sessId,
                            isCorrect);

                } catch (Exception e) {
                    logger.warn(
                            "세션 카운트 업데이트 실패 - ID: {}",
                            sessionId,
                            e);
                }
            }

            /*
             * 사용자 학습 결과 저장
             */
            if (authHeader != null
                    && authHeader.startsWith("Bearer ")) {

                try {
                    Long userId = authUtils.extractUserIdFromAuthHeader(
                            authHeader);

                    WordStudyResult.StudyType type;

                    if (studyType != null
                            && !studyType.trim().isEmpty()) {

                        type = WordStudyResult.StudyType.valueOf(
                                studyType.trim().toUpperCase());

                    } else if (isConjugation) {

                        type = WordStudyResult.StudyType.CONJUGATION;

                    } else {

                        type = WordStudyResult.StudyType.SENTENCE;
                    }

                    studyRecordService.recordWordResult(
                            userId,
                            id,
                            isCorrect,
                            type);

                    logger.info(
                            "학습 결과 저장 완료 - 사용자: {}, 단어: {}, 타입: {}",
                            userId,
                            id,
                            type);

                } catch (Exception e) {
                    logger.warn(
                            "학습 결과 저장 실패 - 단어 ID: {}",
                            id,
                            e);
                }
            }

            /*
             * 응답 생성
             */
            Map<String, Object> response = new HashMap<>();

            response.put("correct", isCorrect);
            response.put("userAnswer", trimmedAnswer);
            response.put("correctAnswer", correctAnswer);

            if (isCorrect) {

                response.put("message", "정답입니다! 🎉");

            } else {

                response.put("message", "틀렸습니다. 다시 시도해보세요. 💪");
                response.put("hint", getHint(word));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            logger.error("정답 확인 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "정답 확인 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 활용형 데이터 유효성 검사
     */
    private boolean isValidForm(String form) {

        if (form == null || form.trim().isEmpty()) {
            return false;
        }

        String value = form.trim();

        return !"null".equalsIgnoreCase(value)
                && !"-".equals(value)
                && !"NaN".equalsIgnoreCase(value);
    }

    /**
     * 일본어 괄호 표기 정규화
     *
     * 明(か)します -> 明かします
     * 荒(ら)します -> 荒らします
     */
    private String normalizeForm(String form) {

        if (form == null) {
            return null;
        }

        return form.trim()
                .replace("(", "")
                .replace(")", "")
                .replace("（", "")
                .replace("）", "");
    }

    /**
     * 오답 힌트 생성
     */
    private String getHint(WordDto word) {

        if (word == null
                || word.getReading() == null
                || word.getReading().isEmpty()) {

            return "";
        }

        String reading = word.getReading();

        if (reading.length() == 1) {
            return "힌트: " + reading;
        }

        return "힌트: " + reading.charAt(0) + "...";
    }

    /**
     * 오답 단어 목록 조회
     */
    @GetMapping("/incorrect")
    public ResponseEntity<List<WordDto>> getIncorrectWords(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader("Authorization") String authHeader) {

        logger.info(
                "틀린 단어 목록 요청 - 레벨: {}, 품사: {}, 타입: {}",
                level, part, studyType);

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(
                    studyType.toUpperCase());

            List<WordDto> words = studyRecordService.getIncorrectWords(
                    userId,
                    level,
                    part,
                    type);

            return ResponseEntity.ok(words);

        } catch (Exception e) {
            logger.error("틀린 단어 목록 조회 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 오답 단어 개수 조회
     */
    @GetMapping("/incorrect/count")
    public ResponseEntity<Map<String, Object>> getIncorrectWordCount(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader("Authorization") String authHeader) {

        logger.info(
                "틀린 단어 개수 조회 - 레벨: {}, 품사: {}, 타입: {}",
                level, part, studyType);

        try {
            if (level == null || level.trim().isEmpty()
                    || part == null || part.trim().isEmpty()
                    || studyType == null || studyType.trim().isEmpty()) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "입력 매개변수를 확인해주세요.");
                error.put("count", 0);

                return ResponseEntity.badRequest().body(error);
            }

            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(
                    studyType.trim().toUpperCase());

            Long count = studyRecordService.getIncorrectWordCount(
                    userId,
                    level.trim(),
                    part.trim(),
                    type);

            Map<String, Object> response = new HashMap<>();

            response.put("count", count != null ? count : 0);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            Map<String, Object> error = new HashMap<>();
            error.put("error", "지원하지 않는 학습 타입입니다.");
            error.put("count", 0);

            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {

            logger.error("틀린 단어 개수 조회 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "서버 오류가 발생했습니다.");
            error.put("count", 0);

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 학습 세션 시작
     */
    @PostMapping("/session/start")
    public ResponseEntity<Map<String, Object>> startStudySession(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            String level = (String) request.get("level");
            String part = (String) request.get("part");
            String studyType = (String) request.get("studyType");

            Boolean isRetrySession = (Boolean) request.getOrDefault(
                    "isRetrySession",
                    false);

            StudySession.StudyType type = StudySession.StudyType.valueOf(
                    studyType.toUpperCase());

            StudySession session = studyRecordService.startStudySession(
                    userId,
                    level,
                    part,
                    type,
                    isRetrySession);

            Map<String, Object> response = new HashMap<>();

            response.put("sessionId", session.getId());
            response.put("message", "학습 세션이 시작되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            logger.error("학습 세션 시작 오류", e);

            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 학습 세션 종료
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endStudySession(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "authToken", required = false) String authToken) {

        logger.info("학습 세션 종료 요청 - ID: {}", sessionId);

        try {
            if (authHeader != null
                    && authHeader.startsWith("Bearer ")) {

                authUtils.extractUserIdFromAuthHeader(authHeader);

            } else if (authToken != null
                    && !authToken.isEmpty()) {

                authUtils.extractUserIdFromToken(authToken);

            } else {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "인증 정보가 필요합니다.");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(error);
            }

            if (sessionId == null || sessionId <= 0) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "잘못된 세션 ID입니다.");

                return ResponseEntity.badRequest().body(error);
            }

            Integer correctCount = (Integer) request.getOrDefault("correctCount", 0);

            Integer incorrectCount = (Integer) request.getOrDefault("incorrectCount", 0);

            Integer totalCount = (Integer) request.getOrDefault("totalCount", 0);

            studyRecordService.endStudySession(
                    sessionId,
                    totalCount,
                    correctCount);

            Map<String, Object> response = new HashMap<>();

            response.put("sessionId", sessionId);
            response.put("correctCount", correctCount);
            response.put("incorrectCount", incorrectCount);
            response.put("totalCount", totalCount);

            response.put(
                    "accuracy",
                    totalCount > 0
                            ? Math.round(
                                    (double) correctCount
                                            / totalCount * 100 * 100.0)
                                    / 100.0
                            : 0.0);

            response.put("message", "학습 세션이 완료되었습니다.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            logger.error("세션 종료 인증 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "인증 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error);

        } catch (Exception e) {

            logger.error("학습 세션 종료 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "학습 세션 종료 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 학습 세션 강제 종료
     */
    @PostMapping("/session/{sessionId}/close")
    public ResponseEntity<Map<String, Object>> closeStudySession(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 세션 강제 종료 - ID: {}", sessionId);

        try {
            authUtils.extractUserIdFromAuthHeader(authHeader);

            if (sessionId == null || sessionId <= 0) {

                Map<String, Object> error = new HashMap<>();
                error.put("error", "잘못된 세션 ID입니다.");

                return ResponseEntity.badRequest().body(error);
            }

            studyRecordService.endStudySessionById(sessionId);

            Map<String, Object> response = new HashMap<>();

            response.put("sessionId", sessionId);
            response.put("message", "학습 세션이 강제 종료되었습니다.");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            Map<String, Object> error = new HashMap<>();
            error.put("error", "인증 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error);

        } catch (Exception e) {

            logger.error("학습 세션 강제 종료 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "학습 세션 강제 종료 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 학습 기록 조회
     */
    @GetMapping("/records")
    public ResponseEntity<List<StudyRecordDto>> getStudyRecords(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 기록 조회 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<StudyRecordDto> records = studyRecordService.getStudyRecords(userId);

            return ResponseEntity.ok(records);

        } catch (Exception e) {
            logger.error("학습 기록 조회 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 개별 학습 결과 저장
     */
    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> saveStudyResult(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 결과 저장 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            Long wordId = Long.valueOf(request.get("wordId").toString());

            Boolean isCorrect = (Boolean) request.get("isCorrect");

            String studyType = (String) request.get("studyType");

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(
                    studyType.toUpperCase());

            studyRecordService.recordWordResult(
                    userId,
                    wordId,
                    isCorrect,
                    type);

            Map<String, Object> response = new HashMap<>();

            response.put("message", "학습 결과가 저장되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            logger.error("학습 결과 저장 오류", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "학습 결과 저장 중 오류가 발생했습니다.");

            return ResponseEntity.internalServerError().body(error);
        }
    }
}
