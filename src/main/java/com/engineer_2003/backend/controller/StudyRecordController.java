package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.StudyRecordDto;
import com.engineer_2003.backend.service.StudyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/study-records")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyRecordController {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordController.class);

    @Autowired
    private StudyRecordService studyRecordService;

    /**
     * 사용자의 학습 기록 조회
     */
    @GetMapping
    public ResponseEntity<List<StudyRecordDto>> getStudyRecords(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("학습 기록 조회 요청");

        try {
            // JWT 토큰에서 사용자 ID 추출 (실제 구현에서는 JWT 파싱 필요)
            Long userId = extractUserIdFromToken(authHeader);

            List<StudyRecordDto> records = studyRecordService.getStudyRecords(userId);

            logger.info("학습 기록 조회 성공 - 개수: {}", records.size());
            return ResponseEntity.ok(records);

        } catch (Exception e) {
            logger.error("학습 기록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (실제 구현에서는 JWT 파싱 로직 필요)
     */
    private Long extractUserIdFromToken(String authHeader) {
        // 실제 구현에서는 JWT 토큰을 파싱해서 사용자 ID를 추출해야 함
        // 여기서는 임시로 고정값 사용
        return 1L;
    }
}