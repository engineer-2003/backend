package com.engineer_2003.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.engineer_2003.backend.dto.StudyRecordResponseDto;
import com.engineer_2003.backend.service.StudyRecordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/study-records")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    // JWT 토큰에서 사용자 ID를 추출하도록 변경
    @GetMapping
    public ResponseEntity<List<StudyRecordResponseDto>> getRecordsByUser(
            @AuthenticationPrincipal User user) {
        
        return ResponseEntity.ok(studyRecordService.getRecordsDtoByUser(user.getUsername()));
    }
}