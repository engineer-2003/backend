package com.engineer_2003.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String googleApiUrl = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    @PostMapping 
    public ChatResponse chatWithGemini(@RequestBody ChatRequest chatRequest) {
        
        Part part = new Part();
        part.setText("あなたは日本語学習を手伝うAIアシスタントです。次の質問に日本語で答えてください: " 
            + chatRequest.getMessage()); 

        Content content = new Content();
        content.setParts(List.of(part));

        GeminiRequest geminiRequest = new GeminiRequest();
        geminiRequest.setContents(List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiRequest, headers);

        try {
            ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(
                googleApiUrl + googleApiKey, 
                entity, 
                GeminiResponse.class
            );

            String aiReply = response.getBody()
                                     .getCandidates()
                                     .get(0)
                                     .getContent()
                                     .getParts()
                                     .get(0)
                                     .getText();
            
            return new ChatResponse(aiReply);

        } catch (Exception e) {
            System.err.println("Google Gemini API 호출 오류: " + e.getMessage());
            return new ChatResponse("죄송합니다. AI 서버(Gemini)에 오류가 발생했습니다.");
        }
    }

    static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    static class ChatResponse {
        private String reply;
        public ChatResponse(String reply) { this.reply = reply; }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
    }

    static class GeminiRequest {
        private List<Content> contents;
        public List<Content> getContents() { return contents; }
        public void setContents(List<Content> contents) { this.contents = contents; }
    }

    static class Content {
        private List<Part> parts;
        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    static class Part {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    static class GeminiResponse {
        private List<Candidate> candidates;
        public List<Candidate> getCandidates() { return candidates; }
        public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }
    }

    static class Candidate {
        private Content content;
        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }
    }
}