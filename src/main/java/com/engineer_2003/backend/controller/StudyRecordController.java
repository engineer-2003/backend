package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.service.StudyRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-records")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    @PostMapping
    public ResponseEntity<StudyRecord> saveRecord(@RequestBody StudyRecord record) {
        return ResponseEntity.ok(studyRecordService.saveRecord(record));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<StudyRecord>> getRecordsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(studyRecordService.getRecordsByUser(userId));
    }
}
