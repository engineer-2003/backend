// src/main/java/com/engineer_2003/backend/controller/StudyController.java
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

    // (getWords, getWord, generateSentenceProblem, generateFormProblem 메서드는 변경 없음)

    @GetMapping
    public ResponseEntity<List<WordDto>> getWords(
            @RequestParam String level,
            @RequestParam String part) {
        logger.info("단어 목록 요청 - 레벨: {}, 품사: {}", level, part);
        try {
            if (level == null || level.trim().isEmpty() || part == null || part.trim().isEmpty()) {
                logger.warn("레벨 또는 품사가 제공되지 않음");
                return ResponseEntity.badRequest().build();
            }
            List<WordDto> words = wordService.getWordsByLevelAndPart(level.trim(), part.trim());
            if (words.isEmpty()) {
                logger.info("해당 조건에 맞는 단어가 없음 - 레벨: {}, 품사: {}", level, part);
                return ResponseEntity.ok(Collections.emptyList());
            }
            logger.info("단어 목록 반환 - 개수: {}", words.size());
            return ResponseEntity.ok(words);
        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<WordDto> getWord(@PathVariable Long id) {
        logger.info("단어 조회 요청 - ID: {}", id);
        try {
            if (id == null || id <= 0) {
                logger.warn("잘못된 단어 ID: {}", id);
                return ResponseEntity.badRequest().build();
            }
            WordDto word = wordService.getWordById(id);
            if (word == null) {
                logger.info("단어를 찾을 수 없음 - ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            logger.info("단어 조회 성공 - ID: {}, 단어: {}", id, word.getWord());
            return ResponseEntity.ok(word);
        } catch (Exception e) {
            logger.error("단어 조회 중 오류 발생 - ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/sentence-problem")
    public ResponseEntity<Map<String, Object>> generateSentenceProblem(@PathVariable Long id) {
        logger.info("예문 문제 생성 요청 - 단어 ID: {}", id);
        try {
            if (id == null || id <= 0) {
                logger.warn("잘못된 단어 ID: {}", id);
                return ResponseEntity.badRequest().build();
            }
            Map<String, Object> problem = wordService.generateSentenceProblem(id);
            if (problem == null || problem.isEmpty()) {
                logger.warn("문제 생성 실패 - 단어 ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "문제를 생성할 수 없습니다.");
                // **수정**: 문제 생성 실패 시 HttpStatus.BAD_REQUEST (400) 반환
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            logger.info("예문 문제 생성 성공 - 단어 ID: {}", id);
            return ResponseEntity.ok(problem);
        } catch (Exception e) {
            logger.error("예문 문제 생성 중 오류 발생 - 단어 ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "문제 생성 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/form-problem")
    public ResponseEntity<Map<String, Object>> generateFormProblem(
            @PathVariable Long id,
            @RequestParam(required = false) String formType) {
        logger.info("활용형 문제 생성 요청 - 단어 ID: {}, 활용형: {}", id, formType);
        try {
            if (id == null || id <= 0) {
                logger.warn("잘못된 단어 ID: {}", id);
                return ResponseEntity.badRequest().build();
            }
            Map<String, Object> problem = wordService.generateFormProblem(id);
            if (problem == null || problem.isEmpty()) {
                logger.warn("활용형 문제 생성 실패 - 단어 ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "활용형 문제를 생성할 수 없습니다.");
                // **수정**: 문제 생성 실패 시 HttpStatus.BAD_REQUEST (400) 반환
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            logger.info("활용형 문제 생성 성공 - 단어 ID: {}", id);
            return ResponseEntity.ok(problem);
        } catch (Exception e) {
            logger.error("활용형 문제 생성 중 오류 발생 - 단어 ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "활용형 문제 생성 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<Map<String, Object>> checkAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("정답 확인 요청 - 단어 ID: {}", id);

        try {
            if (id == null || id <= 0) {
                logger.warn("잘못된 단어 ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "잘못된 단어 ID입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String answer = request.get("answer");
            String formType = request.get("formType");
            String studyType = request.get("studyType");
            String sessionId = request.get("sessionId");

            if (answer == null || answer.trim().isEmpty()) {
                logger.warn("답안이 제공되지 않음");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "답안을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            WordDto word = wordService.getWordById(id);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "단어를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 정답 확인 로직 (기존 로직 유지)
            boolean isCorrect;
            String correctAnswer;

            if (formType == null || formType.trim().isEmpty() || !word.getForms().containsKey(formType)) {
                correctAnswer = word.getWord();
                String reading = word.getReading();
                String trimmedAnswer = answer.trim();
                isCorrect = correctAnswer.equals(trimmedAnswer) || (reading != null && reading.equals(trimmedAnswer));
                logger.info("활용형 없음, 단어/reading 체크 - 단어 ID: {}, 답안: {}", id, answer);
            } else {
                formType = formType.trim();
                correctAnswer = wordService.getCorrectAnswer(id, formType);
                isCorrect = wordService.checkAnswer(id, answer.trim(), formType);
                logger.info("활용형 체크 - 단어 ID: {}, 활용형: {}, 답안: {}", id, formType, answer);
            }

            // userId 추출 (게스트 모드 지원 - 토큰 없으면 0L 반환)
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            // 세션 ID가 있으면 정답/오답 카운트 증가
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                try {
                    Long sessId = Long.valueOf(sessionId);
                    studyRecordService.updateSessionCount(sessId, isCorrect);
                    logger.info("세션 카운트 업데이트 - 세션 ID: {}, 정답 여부: {}", sessId, isCorrect);
                } catch (Exception countError) {
                    logger.warn("세션 카운트 업데이트 실패 - 세션 ID: {}", sessionId, countError);
                }
            }

            // 개별 학습 결과 저장 (게스트 포함)
            try {
                WordStudyResult.StudyType type;

                if (studyType != null && !studyType.trim().isEmpty()) {
                    type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());
                } else if (formType != null && !formType.trim().isEmpty() && word.getForms().containsKey(formType)) {
                    type = WordStudyResult.StudyType.CONJUGATION;
                } else {
                    type = WordStudyResult.StudyType.SENTENCE;
                }

                studyRecordService.recordWordResult(userId, id, isCorrect, type);
                logger.info("개별 학습 결과 저장 완료 - 사용자 ID: {}, 단어 ID: {}, 정답 여부: {}, 타입: {}",
                        userId, id, isCorrect, type);
            } catch (Exception e) {
                logger.warn("개별 학습 결과 저장 실패 - 단어 ID: {}", id, e);
            }


            Map<String, Object> response = new HashMap<>();
            response.put("correct", isCorrect);
            response.put("userAnswer", answer.trim());
            response.put("correctAnswer", correctAnswer);

            if (isCorrect) {
                response.put("message", "정답입니다! 🎉");
            } else {
                response.put("message", "틀렸습니다. 다시 시도해보세요. 💪");
                response.put("hint", getHint(word));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("정답 확인 중 오류 발생 - 단어 ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "정답 확인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private String getHint(WordDto word) {
        if (word == null || word.getReading() == null || word.getReading().isEmpty()) {
            return "";
        }

        String reading = word.getReading();
        if (reading.length() == 1) {
            return "힌트: " + reading;
        }
        return "힌트: " + reading.charAt(0) + "...";
    }

    /**
     * 틀린 단어만 조회하는 API (게스트 포함)
     */
    @GetMapping("/incorrect")
    public ResponseEntity<List<WordDto>> getIncorrectWords(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("틀린 단어 목록 요청 - 레벨: {}, 품사: {}, 타입: {}", level, part, studyType);

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());
            List<WordDto> incorrectWords = studyRecordService.getIncorrectWords(userId, level, part, type);

            logger.info("틀린 단어 목록 반환 - 개수: {}", incorrectWords.size());
            return ResponseEntity.ok(incorrectWords);

        } catch (Exception e) {
            logger.error(" 틀린 단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 틀린 단어 개수 조회 (게스트 포함)
     */
    @GetMapping("/incorrect/count")
    public ResponseEntity<Map<String, Object>> getIncorrectWordCount(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("틀린 단어 개수 조회 요청 - 레벨: {}, 품사: {}, 타입: {}", level, part, studyType);

        try {
            if (level == null || level.trim().isEmpty() || part == null || part.trim().isEmpty() || studyType == null || studyType.trim().isEmpty()) {
                logger.warn("입력 매개변수 중 누락된 항목이 있음");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "입력 매개변수를 확인해주세요.");
                errorResponse.put("count", 0);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            WordStudyResult.StudyType type;
            try {
                type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("지원하지 않는 학습 타입: {}", studyType);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "지원하지 않는 학습 타입입니다.");
                errorResponse.put("count", 0);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Long count = studyRecordService.getIncorrectWordCount(userId, level.trim(), part.trim(), type);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count != null ? count : 0);

            logger.info("틀린 단어 개수 조회 성공 - 사용자: {}, 개수: {}", userId, count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("틀린 단어 개수 조회 중 예외 발생 - 레벨: {}, 품사: {}, 타입: {}", level, part, studyType, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "서버 오류가 발생했습니다.");
            errorResponse.put("count", 0);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 학습 세션 시작
     */
    @PostMapping("/session/start")
    public ResponseEntity<Map<String, Object>> startStudySession(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);
            String level = (String) request.get("level");
            String part = (String) request.get("part");
            String studyType = (String) request.get("studyType");
            Boolean isRetrySession = (Boolean) request.getOrDefault("isRetrySession", false);

            if (userId == AuthUtils.GUEST_USER_ID && isRetrySession) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "게스트 사용자는 재학습 세션을 시작할 수 없습니다. 로그인 후 이용해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            StudySession.StudyType type = StudySession.StudyType.valueOf(studyType.toUpperCase());
            StudySession session = studyRecordService.startStudySession(userId, level, part, type, isRetrySession);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("message", "학습 세션이 시작되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("학습 세션 시작 중 오류 발생", e);
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

        logger.info("학습 세션 종료 요청 - 세션 ID: {}", sessionId);

        try {
            if (sessionId == null || sessionId <= 0) {
                logger.warn("잘못된 세션 ID: {}", sessionId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "잘못된 세션 ID입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Integer correctCount = (Integer) request.getOrDefault("correctCount", 0);
            Integer incorrectCount = (Integer) request.getOrDefault("incorrectCount", 0);
            Integer totalCount = (Integer) request.getOrDefault("totalCount", 0);

            logger.info("학습 결과: 정답={}, 오답={}, 총 문제={}", correctCount, incorrectCount, totalCount);

            studyRecordService.endStudySession(sessionId, totalCount, correctCount);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("correctCount", correctCount);
            response.put("incorrectCount", incorrectCount);
            response.put("totalCount", totalCount);
            response.put("accuracy", totalCount > 0 ? Math.round((double) correctCount / totalCount * 100 * 100.0) / 100.0 : 0.0);
            response.put("message", "학습 세션이 완료되었습니다.");

            logger.info("학습 세션 종료 성공 - 세션 ID: {}, 정답: {}, 오답: {}, 총 문제: {}",
                    sessionId, correctCount, incorrectCount, totalCount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("학습 세션 종료 중 오류 - 세션 ID: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "세션 종료 처리 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("학습 세션 종료 중 오류 발생 - 세션 ID: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학습 세션 종료 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 학습 세션 강제 종료 (뒤로가기 버튼용)
     */
    @PostMapping("/session/{sessionId}/close")
    public ResponseEntity<Map<String, Object>> closeStudySession(
            @PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("학습 세션 강제 종료 요청 - 세션 ID: {}", sessionId);

        try {
            authUtils.extractUserIdFromAuthHeader(authHeader);

            if (sessionId == null || sessionId <= 0) {
                logger.warn("잘못된 세션 ID: {}", sessionId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "잘못된 세션 ID입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            studyRecordService.endStudySessionById(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("message", "학습 세션이 강제 종료되었습니다.");

            logger.info("학습 세션 강제 종료 완료 - 세션 ID: {}", sessionId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("학습 세션 강제 종료 중 오류 - 세션 ID: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "세션 강제 종료 처리 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("학습 세션 강제 종료 중 오류 발생 - 세션 ID: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학습 세션 강제 종료 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 학습 기록 조회
     */
    @GetMapping("/records")
    public ResponseEntity<List<StudyRecordDto>> getStudyRecords(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("학습 기록 조회 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<StudyRecordDto> records = studyRecordService.getStudyRecords(userId);

            logger.info("학습 기록 조회 성공 - 사용자 ID: {}, 기록 개수: {}", userId, records.size());
            return ResponseEntity.ok(records);

        } catch (Exception e) {
            logger.error("학습 기록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 개별 학습 결과 저장
     */
    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> saveStudyResult(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        logger.info("학습 결과 저장 요청");

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            Long wordId = Long.valueOf(request.get("wordId").toString());
            Boolean isCorrect = (Boolean) request.get("isCorrect");
            String studyType = (String) request.get("studyType");

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());

            studyRecordService.recordWordResult(userId, wordId, isCorrect, type);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "학습 결과가 저장되었습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("학습 결과 저장 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학습 결과 저장 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}