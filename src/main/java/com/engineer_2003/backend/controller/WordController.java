package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class WordController {
    private static final Logger logger = LoggerFactory.getLogger(WordController.class);

    @Autowired
    private WordService wordService;

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
                return ResponseEntity.notFound().build();
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
}