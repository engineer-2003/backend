// ChatController.java

package com.engineer_2003.backend.controller;

import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.entity.WordStudyResult;
import com.engineer_2003.backend.repository.StudyRecordRepository;
import com.engineer_2003.backend.repository.WordStudyResultRepository;
import com.engineer_2003.backend.security.AuthUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    @Value("${google.api.key}")
    private String googleApiKey;

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private WordStudyResultRepository wordStudyResultRepository;

    @Autowired
    private AuthUtils authUtils;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> modelUrls = List.of(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=");

    public ChatController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 채팅방 초기 진입 시 실제 학습 단어를 바탕으로 첫 문제 생성
     */
    @GetMapping("/init")
    public ChatResponse initChat(
            @RequestHeader("Authorization") String authHeader) {

        String recentSummary = "";

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

            List<WordStudyResult> wordResults = wordStudyResultRepository.findRecentWordsByUserId(userId);

            if (records != null && !records.isEmpty()) {
                StudyRecord latest = records.get(0);

                String typeLabel = latest.getStudyType() == StudyRecord.StudyType.CONJUGATION
                        ? "활용형 학습"
                        : "예문 학습";

                recentSummary = String.format(
                        "최근에 %s %s(%s)를 공부했네! ",
                        latest.getLevelName(),
                        latest.getPartOfSpeech(),
                        typeLabel);
            }

            String studyContext = buildRecentStudyContext(records, wordResults);

            String systemPrompt = buildSystemPrompt(studyContext);

            Part userPart = new Part();

            userPart.setText(
                    "사용자가 챗봇 학습 화면에 방금 입장했습니다. " +
                            "제공된 실제 학습 단어 목록에서 복습할 단어 하나를 선택하세요. " +
                            "별도의 긴 인사 없이 단어와 읽기, 뜻을 알려주고 " +
                            "해당 단어를 활용할 수 있는 구체적인 상황을 제시하여 " +
                            "일본어 문장을 만들어 보도록 안내해주세요.");

            Content userContent = new Content();
            userContent.setRole("user");
            userContent.setParts(List.of(userPart));

            String aiReply = callGeminiApi(systemPrompt, List.of(userContent));

            return new ChatResponse(aiReply);

        } catch (Exception e) {
            System.err.println(
                    "초기 챗봇 인사 생성 오류: " + e.getMessage());

            return new ChatResponse(
                    "안녕! " + recentSummary +
                            "오늘 복습하고 싶은 일본어 단어나 문장을 말해줄래?");
        }
    }

    /**
     * 사용자 메시지 및 이전 대화 내역을 반영한 챗봇 응답 생성
     */
    @PostMapping
    public ChatResponse chatWithGemini(
            @RequestBody ChatRequest chatRequest,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

            List<WordStudyResult> wordResults = wordStudyResultRepository.findRecentWordsByUserId(userId);

            String studyContext = buildRecentStudyContext(records, wordResults);

            String systemPrompt = buildSystemPrompt(studyContext);

            List<Content> contents = new ArrayList<>();

            String initialBotMessage = null;

            if (chatRequest.getHistory() != null) {

                for (ChatMessageDto msg : chatRequest.getHistory()) {

                    String role = "bot".equals(msg.getSender())
                            ? "model"
                            : "user";

                    // 첫 번째 메시지가 봇의 인사말이면 별도로 보관
                    if (contents.isEmpty()
                            && "model".equals(role)) {

                        initialBotMessage = msg.getText();
                        continue;
                    }

                    Part p = new Part();
                    p.setText(msg.getText());

                    Content c = new Content();
                    c.setRole(role);
                    c.setParts(List.of(p));

                    contents.add(c);
                }
            }

            // 대화 내역이 없는 경우 기본 user 메시지 생성
            if (contents.isEmpty() && initialBotMessage == null) {

                Part p = new Part();
                p.setText("일본어 작문 복습을 시작할게.");

                Content c = new Content();
                c.setRole("user");
                c.setParts(List.of(p));

                contents.add(c);
            }

            // 최초 봇 메시지를 대화 맥락에 포함
            if (initialBotMessage != null) {

                if (contents.isEmpty()) {

                    Part p = new Part();

                    p.setText(
                            "[이전 챗봇의 첫 번째 출제 내용]\n" +
                                    initialBotMessage);

                    Content c = new Content();
                    c.setRole("user");
                    c.setParts(List.of(p));

                    contents.add(c);

                } else if ("user".equals(contents.get(0).getRole())) {

                    String originalText = contents.get(0).getParts()
                            .get(0).getText();

                    contents.get(0).getParts()
                            .get(0).setText(
                                    "[이전 챗봇의 첫 번째 출제 내용]\n" +
                                            initialBotMessage +
                                            "\n\n[사용자의 답변]\n" +
                                            originalText);
                }
            }

            // 현재 사용자 메시지 추가
            Part currentPart = new Part();
            currentPart.setText(chatRequest.getMessage());

            Content currentContent = new Content();
            currentContent.setRole("user");
            currentContent.setParts(List.of(currentPart));

            contents.add(currentContent);

            String aiReply = callGeminiApi(systemPrompt, contents);

            return new ChatResponse(aiReply);

        } catch (Exception e) {

            System.err.println(
                    "Google Gemini API 호출 오류: " + e.getMessage());

            return new ChatResponse(
                    "죄송합니다. 현재 AI 서버 무료 요청 한도가 초과되었거나 " +
                            "응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 최근 7일간의 학습 기록을 바탕으로 AI 주간 요약 코멘트 생성
     */
    @GetMapping("/weekly-summary")
    public ResponseEntity<WeeklySummaryResponse> getWeeklySummary(
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long userId = authUtils.extractUserIdFromAuthHeader(authHeader);

            List<StudyRecord> allRecords = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

            LocalDate today = LocalDate.now();
            LocalDate sevenDaysAgo = today.minusDays(6);

            List<StudyRecord> recentRecords = allRecords.stream()
                    .filter(r -> r.getStudyDate() != null
                            && !r.getStudyDate().isBefore(sevenDaysAgo)
                            && !r.getStudyDate().isAfter(today))
                    .collect(Collectors.toList());

            if (recentRecords.isEmpty()) {
                return ResponseEntity.ok(createEmptyWeeklySummary());
            }

            StringBuilder sb = new StringBuilder();

            for (StudyRecord r : recentRecords) {

                String dayOfWeek = r.getStudyDate().getDayOfWeek()
                        .getDisplayName(
                                TextStyle.SHORT,
                                Locale.KOREAN);

                String typeLabel = r.getStudyType() == StudyRecord.StudyType.CONJUGATION
                        ? "활용형 학습"
                        : "예문 학습";

                int wordCount = r.getWordCount() != null
                        ? r.getWordCount()
                        : 0;

                int correctCount = r.getCorrectCount() != null
                        ? r.getCorrectCount()
                        : 0;

                int accuracy = wordCount > 0
                        ? (int) Math.round(
                                (correctCount * 100.0) / wordCount)
                        : 0;

                int studyTime = r.getStudyTime() != null
                        ? r.getStudyTime()
                        : 0;

                sb.append(String.format(
                        "- %s(%s요일) | 급수: %s | 품사: %s | 유형: %s | 단어: %d개 | 정답: %d개 (정답률 %d%%) | 학습시간: %d초\n",
                        r.getStudyDate(),
                        dayOfWeek,
                        r.getLevelName(),
                        r.getPartOfSpeech(),
                        typeLabel,
                        wordCount,
                        correctCount,
                        accuracy,
                        studyTime));
            }

            String systemPrompt = "당신은 한국인 일본어 학습자의 최근 7일 학습 데이터를 분석해 따뜻하고 구체적인 주간 리포트를 작성하는 AI 튜터입니다.\n" +
                    "반드시 아래 JSON 형식으로만 응답하세요. 마크다운 코드블록, 이모지, 마크다운 기호를 포함하지 마세요.\n" +
                    "{\n" +
                    "  \"headline\": \"한 줄 요약 제목 (25자 이내, 해요체)\",\n" +
                    "  \"comment\": \"최근 7일간의 학습 일수, 요일별 패턴, 정답률과 학습 시간 특징을 담은 2~3문장 총평 (해요체)\",\n" +
                    "  \"points\": [\n" +
                    "    { \"icon\": \"trend\", \"title\": \"강점\", \"text\": \"데이터에서 드러난 잘한 점 1~2문장 (해요체)\" },\n" +
                    "    { \"icon\": \"target\", \"title\": \"보완할 점\", \"text\": \"정답률이 낮았거나 복습이 필요한 요일·품사·유형에 대한 구체적 조언 1~2문장 (해요체)\" },\n"
                    +
                    "    { \"icon\": \"bulb\", \"title\": \"다음 주 추천\", \"text\": \"최근 학습한 급수·품사를 바탕으로 한 다음 주 실천 목표 제안 1~2문장 (해요체)\" }\n"
                    +
                    "  ]\n" +
                    "}";

            Part userPart = new Part();

            userPart.setText(
                    "[최근 7일 학습 기록]\n" +
                            sb +
                            "\n위 데이터를 분석해 JSON 형식의 주간 요약 코멘트를 작성해주세요.");

            Content userContent = new Content();
            userContent.setRole("user");
            userContent.setParts(List.of(userPart));

            String rawReply = callGeminiApi(
                    systemPrompt,
                    List.of(userContent));

            String cleanedJson = rawReply.trim();

            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson
                        .replaceAll("^```(?:json)?\\s*", "")
                        .replaceAll("\\s*```$", "")
                        .trim();
            }

            WeeklySummaryResponse summary = objectMapper.readValue(
                    cleanedJson,
                    WeeklySummaryResponse.class);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {

            System.err.println(
                    "AI 주간 요약 생성 오류: " + e.getMessage());

            return ResponseEntity.internalServerError().build();
        }
    }

    private WeeklySummaryResponse createEmptyWeeklySummary() {

        WeeklySummaryResponse res = new WeeklySummaryResponse();

        res.setHeadline("이번 주 학습을 시작해보세요!");

        res.setComment(
                "아직 최근 7일간의 학습 기록이 없어요. " +
                        "오늘 첫 단어 학습을 진행해보세요.");

        res.setPoints(List.of(
                new SummaryPointDto(
                        "trend",
                        "강점",
                        "꾸준한 학습 기록이 쌓이면 강점 요일을 분석해드려요."),
                new SummaryPointDto(
                        "target",
                        "보완할 점",
                        "정답률이 낮은 요일의 단어를 복습 퀴즈로 다시 확인해보세요."),
                new SummaryPointDto(
                        "bulb",
                        "다음 주 추천",
                        "하루 15문제씩 목표를 설정하고 연속 학습일을 늘려보세요.")));

        return res;
    }

    /**
     * 실제 학습 기록과 학습 단어를 AI 프롬프트용 텍스트로 변환
     */
    private String buildRecentStudyContext(
            List<StudyRecord> records,
            List<WordStudyResult> wordResults) {

        StringBuilder sb = new StringBuilder();

        if (records != null && !records.isEmpty()) {

            sb.append("[최근 학습 기록]\n");

            List<StudyRecord> recentRecords = records.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            for (StudyRecord r : recentRecords) {

                String typeLabel = r.getStudyType() == StudyRecord.StudyType.CONJUGATION
                        ? "활용형 학습"
                        : "예문 학습";

                sb.append(String.format(
                        "- 날짜: %s | 급수: %s | 품사: %s | 유형: %s | 학습 단어 수: %d개 (정답: %d개)\n",
                        r.getStudyDate(),
                        r.getLevelName(),
                        r.getPartOfSpeech(),
                        typeLabel,
                        r.getWordCount() != null
                                ? r.getWordCount()
                                : 0,
                        r.getCorrectCount() != null
                                ? r.getCorrectCount()
                                : 0));
            }
        }

        if (wordResults == null || wordResults.isEmpty()) {

            sb.append(
                    "\n[실제 학습 단어]\n" +
                            "아직 조회할 수 있는 학습 단어가 없습니다.\n");

            return sb.toString();
        }

        sb.append("\n[사용자가 실제로 학습한 단어 목록]\n");
        sb.append("아래 단어만 이번 복습의 출제 대상으로 사용할 수 있습니다.\n");

        Set<Long> seenWordIds = new HashSet<>();

        int count = 0;

        for (WordStudyResult result : wordResults) {

            if (result.getWord() == null) {
                continue;
            }

            if (!seenWordIds.add(result.getWordId())) {
                continue;
            }

            if (count >= 30) {
                break;
            }

            var word = result.getWord();

            count++;

            sb.append(String.format(
                    "%d. 단어: %s | 읽기: %s | 뜻: %s | 급수: %s | 품사: %s | 학습 유형: %s | 정답 여부: %s\n",
                    count,
                    word.getWord(),
                    word.getReading(),
                    word.getMeaning(),
                    word.getLevelName(),
                    word.getPartOfSpeech(),
                    result.getStudyType(),
                    Boolean.TRUE.equals(result.getIsCorrect())
                            ? "정답"
                            : "오답 또는 미완료"));

            if (word.getExampleSentence() != null
                    && !word.getExampleSentence().isBlank()) {

                sb.append(
                        "   기존 예문: " +
                                word.getExampleSentence());

                if (word.getExampleTranslation() != null) {
                    sb.append(
                            " (" +
                                    word.getExampleTranslation() +
                                    ")");
                }

                sb.append("\n");
            }
        }

        sb.append(
                "\n총 " + count + "개의 실제 학습 단어를 조회했습니다.\n");

        return sb.toString();
    }

    /**
     * 일본어 작문 튜터 시스템 프롬프트
     */
    private String buildSystemPrompt(String studyContext) {

        return "당신은 한국인 일본어 학습자의 복습을 돕는 차분하고 친근한 AI 튜터입니다.\n" +
                "친근한 반말을 사용하며 깔끔하고 읽기 쉬운 한국어로 설명하세요.\n\n" +

                "[사용자의 학습 데이터]\n" +
                studyContext + "\n" +

                "[가장 중요한 학습 원칙]\n" +
                "1. 반드시 실제 학습 단어 목록에 포함된 단어를 출제 대상으로 선택하세요.\n" +
                "2. 목록에 없는 단어를 사용자가 학습한 단어인 것처럼 제시하지 마세요.\n" +
                "3. 급수와 품사만 참고하여 임의의 단어를 출제하지 마세요.\n" +
                "4. 단어의 실제 의미와 품사를 고려하여 자연스러운 상황을 만들어 주세요.\n" +
                "5. 사용자가 해당 단어를 일본어 문장에 직접 활용하도록 유도하세요.\n" +
                "6. 단순한 뜻풀이보다 문맥을 이해하고 문장을 직접 작문하는 연습에 집중하세요.\n\n" +

                "[튜터링 진행 방식]\n" +

                "1. 첫 문제 출제\n" +
                "실제 학습 단어 중 하나를 선택하세요. 단어와 읽기, 뜻을 제시하고 구체적인 상황을 설명하세요.\n" +
                "단어의 특성에 맞는 활용형이나 문법 표현을 지정할 수 있습니다.\n" +
                "모든 단어에 동일한 활용형을 강제하지 마세요.\n\n" +

                "출제 예시:\n" +
                "오늘 복습할 첫 번째 단어는 「清々しい（すがすがしい）」야. '상쾌하다'라는 뜻이지.\n" +
                "내일 아침 날씨를 추측하는 상황을 생각해 보자. 미연형을 활용해서 내일 아침 날씨가 어떨지 일본어 문장으로 만들어 볼까?\n\n" +

                "2. 사용자 답변 평가\n" +
                "다음 기준으로 문장을 평가하세요.\n" +
                "- 학습 단어를 실제로 사용했는가?\n" +
                "- 단어의 의미를 문맥에 맞게 사용했는가?\n" +
                "- 활용형과 문법이 정확한가?\n" +
                "- 일본어 원어민 관점에서 자연스러운가?\n" +
                "- 문어체와 회화체의 차이가 있는가?\n\n" +

                "문법적으로 정확하다면 긍정적인 피드백을 제공하세요.\n" +
                "문법적으로 틀렸다면 틀린 부분과 이유를 설명하세요.\n" +
                "문법적으로 맞더라도 어색한 표현이라면 자연스러운 대안을 제안하세요.\n" +
                "여러 표현이 가능하다면 하나만 정답으로 단정하지 마세요.\n" +
                "학습 단어를 사용하지 않았다면 해당 단어를 활용하도록 다시 안내하세요.\n\n" +

                "피드백 예시:\n" +
                "좋아! 「내일 아침은 상쾌하겠지」라는 의미로, 미연형을 활용해 추측의 뜻을 잘 표현했네!\n" +
                "다만 「清々しかろう」는 문어체적인 표현이야. 일상 회화에서는 「清々しいだろう」라고 표현하는 것이 더 자연스러워.\n" +
                "문맥에 따라 표현이 달라질 수 있다는 점도 기억해 두자!\n\n" +

                "3. 다음 문제 진행\n" +
                "피드백이 끝나면 자연스럽게 다음 학습 단어 또는 다른 활용 문제로 넘어가세요.\n" +
                "대화 내역에서 이미 출제한 단어를 확인하고 가급적 반복하지 마세요.\n" +
                "아직 출제하지 않은 학습 단어를 우선 선택하세요.\n" +
                "한 번에 여러 단어를 출제하지 말고 하나씩 진행하세요.\n" +
                "모든 단어를 출제했다면 이를 알려주고 복습이 필요한 단어를 다시 선택하세요.\n\n" +

                "[출력 형식 필수 규칙]\n" +
                "1. 마크다운 문법을 사용하지 마세요.\n" +
                "2. 이모지와 특수 기호 장식을 사용하지 마세요.\n" +
                "3. 일본어 단어, 예문, 한국어 해석은 낫표 「」로 표시하세요.\n" +
                "4. 한자 단어에는 괄호로 읽기를 병기하세요.\n" +
                "5. 모바일 채팅창에서 읽기 편하도록 3~5문장 이내로 작성하세요.\n" +
                "6. 문맥이 바뀔 때만 한 줄을 띄우세요.\n\n" +

                "[학습 단어가 없는 경우]\n" +
                "실제 학습 단어 목록이 비어 있다면 임의의 단어를 출제하지 마세요.\n" +
                "먼저 단어 학습을 진행한 뒤 다시 복습할 수 있도록 안내하세요.";
    }

    /**
     * Gemini API 호출 및 모델별 재시도
     */
    private String callGeminiApi(
            String systemPrompt,
            List<Content> contents) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Exception lastException = null;
        int maxRetries = 2;

        for (String url : modelUrls) {

            String modelName = url.substring(
                    url.indexOf("models/"),
                    url.indexOf(":generateContent"));

            for (int attempt = 1; attempt <= maxRetries; attempt++) {

                try {

                    GeminiRequest geminiRequest = new GeminiRequest();

                    if (url.contains("gemma")) {

                        List<Content> mergedContents = new ArrayList<>();

                        Part sysMergedPart = new Part();

                        String firstUserText = contents.get(0)
                                .getParts()
                                .get(0)
                                .getText();

                        sysMergedPart.setText(
                                systemPrompt +
                                        "\n\n---\n" +
                                        firstUserText);

                        Content firstMergedContent = new Content();

                        firstMergedContent.setRole("user");
                        firstMergedContent.setParts(
                                List.of(sysMergedPart));

                        mergedContents.add(firstMergedContent);

                        for (int i = 1; i < contents.size(); i++) {
                            mergedContents.add(contents.get(i));
                        }

                        geminiRequest.setContents(mergedContents);

                    } else {

                        Part sysPart = new Part();
                        sysPart.setText(systemPrompt);

                        Content sysContent = new Content();
                        sysContent.setParts(List.of(sysPart));

                        geminiRequest.setSystemInstruction(sysContent);
                        geminiRequest.setContents(contents);
                    }

                    HttpEntity<GeminiRequest> entity = new HttpEntity<>(
                            geminiRequest,
                            headers);

                    ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(
                            url + googleApiKey,
                            entity,
                            GeminiResponse.class);

                    return extractReplyText(response.getBody());

                } catch (Exception e) {

                    lastException = e;

                    System.err.println(
                            "모델 호출 실패 (" +
                                    modelName +
                                    ", 시도 " +
                                    attempt +
                                    "/" +
                                    maxRetries +
                                    "): " +
                                    e.getMessage());

                    if (attempt < maxRetries) {

                        try {
                            Thread.sleep(1500);

                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw ie;
                        }
                    }
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IllegalStateException(
                "사용 가능한 Gemini 모델이 없습니다.");
    }

    private String extractReplyText(GeminiResponse body) {

        if (body == null
                || body.getCandidates() == null
                || body.getCandidates().isEmpty()
                || body.getCandidates().get(0).getContent() == null
                || body.getCandidates().get(0).getContent().getParts() == null
                || body.getCandidates().get(0).getContent().getParts().isEmpty()) {

            throw new IllegalStateException(
                    "Gemini API에서 유효한 응답을 받지 못했습니다.");
        }

        return body.getCandidates()
                .get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();
    }

    // --- DTOs ---

    static class ChatRequest {

        private String message;
        private List<ChatMessageDto> history;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<ChatMessageDto> getHistory() {
            return history;
        }

        public void setHistory(List<ChatMessageDto> history) {
            this.history = history;
        }
    }

    static class ChatMessageDto {

        private String sender;
        private String text;

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    static class ChatResponse {

        private String reply;

        public ChatResponse(String reply) {
            this.reply = reply;
        }

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }
    }

    static class WeeklySummaryResponse {

        private String headline;
        private String comment;
        private List<SummaryPointDto> points;

        public String getHeadline() {
            return headline;
        }

        public void setHeadline(String headline) {
            this.headline = headline;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public List<SummaryPointDto> getPoints() {
            return points;
        }

        public void setPoints(List<SummaryPointDto> points) {
            this.points = points;
        }
    }

    static class SummaryPointDto {

        private String icon;
        private String title;
        private String text;

        public SummaryPointDto() {
        }

        public SummaryPointDto(
                String icon,
                String title,
                String text) {

            this.icon = icon;
            this.title = title;
            this.text = text;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    static class GeminiRequest {

        private Content system_instruction;
        private List<Content> contents;

        public Content getSystem_instruction() {
            return system_instruction;
        }

        public void setSystemInstruction(Content systemInstruction) {
            this.system_instruction = systemInstruction;
        }

        public List<Content> getContents() {
            return contents;
        }

        public void setContents(List<Content> contents) {
            this.contents = contents;
        }
    }

    static class Content {

        private String role;
        private List<Part> parts;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    static class Part {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    static class GeminiResponse {

        private List<Candidate> candidates;

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Candidate> candidates) {
            this.candidates = candidates;
        }
    }

    static class Candidate {

        private Content content;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }
    }
}
