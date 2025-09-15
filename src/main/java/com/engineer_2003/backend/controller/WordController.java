package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.StudyRecordDto;
import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.StudySession;
import com.engineer_2003.backend.entity.User;
import com.engineer_2003.backend.entity.WordStudyResult;
import com.engineer_2003.backend.repository.UserRepository;
import com.engineer_2003.backend.service.StudyRecordService;
import com.engineer_2003.backend.service.WordService;
import com.engineer_2003.backend.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/study")
@CrossOrigin(origins = "http://localhost:5173")
public class WordController {
    private static final Logger logger = LoggerFactory.getLogger(WordController.class);

    @Autowired
    private WordService wordService;

    @Autowired
    private StudyRecordService studyRecordService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<WordDto>> getWords(
            @RequestParam String level,
            @RequestParam String part) {

        logger.info("단어 목록 요청 - 레벨: {}, 품사: {}", level, part);

        try {
            // 입력값 검증
            if (level == null || level.trim().isEmpty()) {
                logger.warn("레벨이 제공되지 않음");
                return ResponseEntity.badRequest().build();
            }

            if (part == null || part.trim().isEmpty()) {
                logger.warn("품사가 제공되지 않음");
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

    /**
     * 예문 기반 문제 생성 API
     */
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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

    /**
     * 활용형 문제 생성 API
     */
    @GetMapping("/{id}/form-problem")
    public ResponseEntity<Map<String, Object>> generateFormProblem(@PathVariable Long id) {
        logger.info("활용형 문제 생성 요청 - 단어 ID: {}", id);

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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
            @RequestBody Map<String, String> request) {

        logger.info("정답 확인 요청 - 단어 ID: {}", id);

        try {
            // 입력값 검증
            if (id == null || id <= 0) {
                logger.warn("잘못된 단어 ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "잘못된 단어 ID입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String answer = request.get("answer");
            String formType = request.get("formType"); // 활용형

            if (answer == null || answer.trim().isEmpty()) {
                logger.warn("답안이 제공되지 않음");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "답안을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 단어 존재 확인
            WordDto word = wordService.getWordById(id);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "단어를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 활용형이 없거나 formType이 빈 문자열이면 단어 자체를 정답으로 체크
            boolean isCorrect;
            String correctAnswer;

            if (formType == null || formType.trim().isEmpty() || !word.getForms().containsKey(formType)) {
                correctAnswer = word.getWord();
                String reading = word.getReading(); // 요미가나
                String trimmedAnswer = answer.trim();

                // 정답은 단어 자체 또는 reading과 같으면 정답 처리
                isCorrect = correctAnswer.equals(trimmedAnswer) || (reading != null && reading.equals(trimmedAnswer));

                logger.info("활용형 없음, 단어/reading 체크 - 단어 ID: {}, 답안: {}", id, answer);
            } else {
                // 활용형 체크
                formType = formType.trim();
                correctAnswer = wordService.getCorrectAnswer(id, formType);
                isCorrect = wordService.checkAnswer(id, answer.trim(), formType);
                logger.info("활용형 체크 - 단어 ID: {}, 활용형: {}, 답안: {}", id, formType, answer);
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

    /**
     * 힌트 생성 (reading의 첫 글자만 보여주기)
     */
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
     * 틀린 단어만 조회하는 API
     */
    @GetMapping("/incorrect")
    public ResponseEntity<List<WordDto>> getIncorrectWords(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("틀린 단어 목록 요청 - 레벨: {}, 품사: {}, 타입: {}", level, part, studyType);

        try {
            // JWT 토큰에서 사용자 ID 추출
            Long userId = extractUserIdFromToken(authHeader);

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());
            List<WordDto> incorrectWords = studyRecordService.getIncorrectWords(userId, level, part, type);

            logger.info("틀린 단어 목록 반환 - 개수: {}", incorrectWords.size());
            return ResponseEntity.ok(incorrectWords);

        } catch (Exception e) {
            logger.error("틀린 단어 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 틀린 단어 개수 조회
     */
    @GetMapping("/incorrect/count")
    public ResponseEntity<Map<String, Object>> getIncorrectWordCount(
            @RequestParam String level,
            @RequestParam String part,
            @RequestParam String studyType,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = extractUserIdFromToken(authHeader);
            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());

            Long count = studyRecordService.getIncorrectWordCount(userId, level, part, type);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("틀린 단어 개수 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
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
            Long userId = extractUserIdFromToken(authHeader);
            String level = (String) request.get("level");
            String part = (String) request.get("part");
            String studyType = (String) request.get("studyType");
            Boolean isRetrySession = (Boolean) request.getOrDefault("isRetrySession", false);

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
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 세션 종료 요청 - 세션 ID: {}", sessionId);

        try {
            Long userId = extractUserIdFromToken(authHeader);

            // 세션 ID 검증
            if (sessionId == null || sessionId <= 0) {
                logger.warn("잘못된 세션 ID: {}", sessionId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "잘못된 세션 ID입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 학습 결과 데이터 추출
            Integer correctCount = (Integer) request.getOrDefault("correctCount", 0);
            Integer incorrectCount = (Integer) request.getOrDefault("incorrectCount", 0);
            Integer totalCount = (Integer) request.getOrDefault("totalCount", 0);

            // StudyRecordService의 endStudySession 메서드 호출 (3개 매개변수)
            studyRecordService.endStudySession(sessionId, totalCount, correctCount);

            // 개별 단어 결과 처리 (선택사항)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> studyResults = (List<Map<String, Object>>) request.get("studyResults");
            if (studyResults != null) {
                for (Map<String, Object> result : studyResults) {
                    Long wordId = Long.valueOf(result.get("wordId").toString());
                    Boolean isCorrect = (Boolean) result.get("isCorrect");
                    String studyTypeStr = (String) result.get("studyType");

                    WordStudyResult.StudyType studyType = WordStudyResult.StudyType.valueOf(studyTypeStr.toUpperCase());
                    studyRecordService.recordWordResult(userId, wordId, isCorrect, studyType);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("correctCount", correctCount);
            response.put("incorrectCount", incorrectCount);
            response.put("totalCount", totalCount);
            response.put("accuracy", totalCount > 0 ? (double) correctCount / totalCount * 100 : 0.0);
            response.put("message", "학습 세션이 완료되었습니다.");

            logger.info("학습 세션 종료 성공 - 세션 ID: {}, 정답: {}, 오답: {}, 총 문제: {}",
                    sessionId, correctCount, incorrectCount, totalCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("학습 세션 종료 중 오류 발생 - 세션 ID: {}", sessionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "학습 세션 종료 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 학습 기록 조회
     */
    @GetMapping("/records")
    public ResponseEntity<List<StudyRecordDto>> getStudyRecords(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("학습 기록 조회 요청 - 페이지: {}, 크기: {}", page, size);

        try {
            Long userId = extractUserIdFromToken(authHeader);
            // StudyRecordService의 getStudyRecords는 userId만 받습니다
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
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 결과 저장 요청");

        try {
            Long userId = extractUserIdFromToken(authHeader);
            Long wordId = Long.valueOf(request.get("wordId").toString());
            Boolean isCorrect = (Boolean) request.get("isCorrect");
            String studyType = (String) request.get("studyType");

            WordStudyResult.StudyType type = WordStudyResult.StudyType.valueOf(studyType.toUpperCase());

            // StudyRecordService의 recordWordResult 메서드 사용
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

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 Authorization 헤더입니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
        }

        String username = getUsernameFromToken(token);
        return getUserIdByUsername(username);
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    private String getUsernameFromToken(String token) {
        try {
            // JwtTokenProvider의 getAuthentication을 통해 사용자명을 추출하는 방법
            // 또는 직접 토큰을 파싱하는 방법을 사용
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            return auth.getName();
        } catch (Exception e) {
            throw new IllegalArgumentException("토큰에서 사용자명을 추출할 수 없습니다.", e);
        }
    }

    /**
     * username으로 사용자 ID 조회
     */
    private Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username);
        }
        return user.getUserId();
    }
}