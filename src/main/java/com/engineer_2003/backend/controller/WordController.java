package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study")
@CrossOrigin(origins = "http://localhost:5173")
public class WordController {

    @Autowired
    private WordService wordService;

    @GetMapping
    public ResponseEntity<List<WordDto>> getWords(
            @RequestParam String level,
            @RequestParam String part) {
        List<WordDto> words = wordService.getWordsByLevelAndPart(level, part);
        return ResponseEntity.ok(words);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WordDto> getWord(@PathVariable Long id) {
        WordDto word = wordService.getWordById(id);
        if (word == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(word);
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<Map<String, Object>> checkAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String answer = request.get("answer");
        String formType = request.get("formType");

        boolean isCorrect = wordService.checkAnswer(id, answer, formType);

        Map<String, Object> response = Map.of(
                "correct", isCorrect,
                "message", isCorrect ? "정답입니다!" : "틀렸습니다. 다시 시도해보세요."
        );

        return ResponseEntity.ok(response);
    }
}
