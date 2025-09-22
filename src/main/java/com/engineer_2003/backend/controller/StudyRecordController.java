package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.StudyRecordDto;
import com.engineer_2003.backend.entity.User;
import com.engineer_2003.backend.repository.UserRepository;
import com.engineer_2003.backend.service.StudyRecordService;
import com.engineer_2003.backend.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyRecordController {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordController.class);

    @Autowired
    private StudyRecordService studyRecordService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자의 학습 기록 조회 (초단위 지원)
     */
    @GetMapping("/study-records")
    public ResponseEntity<List<StudyRecordDto>> getStudyRecords(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 기록 조회 요청");

        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<StudyRecordDto> records = studyRecordService.getStudyRecords(userId);

            logger.info("학습 기록 조회 성공 - 사용자 ID: {}, 기록 개수: {}", userId, records.size());
            return ResponseEntity.ok(records);

        } catch (Exception e) {
            logger.error("학습 기록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 주간 통계 조회 (최근 7일)
     */
    @GetMapping("/study-records/weekly-stats")
    public ResponseEntity<Map<String, Object>> getWeeklyStats(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("주간 통계 조회 요청");

        try {
            Long userId = extractUserIdFromToken(authHeader);

            LocalDate today = LocalDate.now();
            LocalDate sevenDaysAgo = today.minusDays(6);

            // 최근 7일간의 총 학습 시간 (초 단위)
            Integer totalTimeInSeconds = studyRecordService.getTotalStudyTimeInSeconds(userId, sevenDaysAgo, today);

            // 최근 7일간의 학습 기록
            List<StudyRecordDto> recentRecords = studyRecordService.getStudyRecords(userId).stream()
                    .filter(record -> {
                        LocalDate recordDate = LocalDate.parse(record.getStudyDate());
                        return !recordDate.isBefore(sevenDaysAgo) && !recordDate.isAfter(today);
                    })
                    .toList();

            // 요일별 통계 계산 (일요일=0, 월요일=1, ..., 토요일=6)
            int[] weekStats = new int[7];
            for (StudyRecordDto record : recentRecords) {
                LocalDate recordDate = LocalDate.parse(record.getStudyDate());
                int dayOfWeek = recordDate.getDayOfWeek().getValue() % 7; // 월요일=1을 0으로, 일요일=7을 0으로
                weekStats[dayOfWeek] += record.getStudyTime();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalTimeInSeconds", totalTimeInSeconds != null ? totalTimeInSeconds : 0);
            response.put("weekStats", weekStats);
            response.put("message", "주간 통계 조회 성공");

            logger.info("주간 통계 조회 성공 - 사용자 ID: {}, 총 학습 시간: {}초", userId, totalTimeInSeconds);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("주간 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 기간의 학습 통계 조회
     */
    @GetMapping("/study-records/period-stats")
    public ResponseEntity<Map<String, Object>> getPeriodStats(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        logger.info("기간별 통계 조회 요청 - 시작: {}, 종료: {}", startDate, endDate);

        try {
            Long userId = extractUserIdFromToken(authHeader);

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            Integer totalTimeInSeconds = studyRecordService.getTotalStudyTimeInSeconds(userId, start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("totalTimeInSeconds", totalTimeInSeconds != null ? totalTimeInSeconds : 0);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("message", "기간별 통계 조회 성공");

            logger.info("기간별 통계 조회 성공 - 사용자 ID: {}, 총 학습 시간: {}초", userId, totalTimeInSeconds);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("기간별 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 Authorization 헤더입니다.");
        }

        String token = authHeader.substring(7);

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