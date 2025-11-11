// StudyRecordService.java
package com.engineer_2003.backend.service;

import com.engineer_2003.backend.dto.StudyRecordDto;
import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.entity.StudySession;
import com.engineer_2003.backend.entity.WordStudyResult;
import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.repository.StudyRecordRepository;
import com.engineer_2003.backend.repository.StudySessionRepository;
import com.engineer_2003.backend.repository.WordStudyResultRepository;
import com.engineer_2003.backend.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudyRecordService {

    private static final Logger logger = LoggerFactory.getLogger(StudyRecordService.class);

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    @Autowired
    private WordStudyResultRepository wordStudyResultRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private WordRepository wordRepository;

    /**
     * 학습 세션 시작
     */
    public StudySession startStudySession(Long userId, String levelName, String partOfSpeech,
                                          StudySession.StudyType studyType, boolean isRetrySession) {
        logger.info("학습 세션 시작 - 사용자: {}, 레벨: {}, 품사: {}, 타입: {}, 재학습: {}",
                userId, levelName, partOfSpeech, studyType, isRetrySession);

        StudySession session = StudySession.builder()
                .userId(userId)
                .levelName(levelName)
                .partOfSpeech(partOfSpeech)
                .studyType(studyType)
                .isRetrySession(isRetrySession)
                .startTime(LocalDateTime.now())
                .currentCorrectCount(0)
                .currentIncorrectCount(0)
                .build();

        return studySessionRepository.save(session);
    }

    /**
     * 세션의 정답/오답 카운트 업데이트
     */
    @Transactional
    public void updateSessionCount(Long sessionId, boolean isCorrect) {
        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            return;
        }

        StudySession session = sessionOpt.get();
        if (isCorrect) {
            session.incrementCorrectCount();
        } else {
            session.incrementIncorrectCount();
        }

        studySessionRepository.save(session);
        logger.debug("세션 카운트 업데이트 - 세션 ID: {}, 정답: {}, 오답: {}",
                sessionId, session.getCurrentCorrectCount(), session.getCurrentIncorrectCount());
    }

    /**
     * 학습 세션 종료 및 기록 저장 (완료 시)
     */
    @Transactional
    public void endStudySession(Long sessionId, int totalQuestions, int correctAnswers) {
        logger.info("학습 세션 종료 - 세션 ID: {}, 총 문제: {}, 정답: {}", sessionId, totalQuestions, correctAnswers);

        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }

        StudySession session = sessionOpt.get();

        // end_time이 이미 설정되어 있으면 중복 처리 방지
        if (session.getEndTime() != null) {
            logger.warn("이미 종료된 세션 - 세션 ID: {}, 기존 종료 시간: {}", sessionId, session.getEndTime());
            return;
        }

        // 현재 시간으로 종료 시간 설정
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);

        // 프론트엔드에서 전달받은 값 대신 세션에 누적된 실시간 카운트를 사용
        int actualQuestions = session.getTotalCurrentQuestions();
        int actualCorrect = session.getCurrentCorrectCount();

        session.setTotalQuestions(actualQuestions);
        session.setCorrectAnswers(actualCorrect);

        // 세션 저장
        studySessionRepository.save(session);
        logger.info("세션 종료 시간 설정 완료 - 세션 ID: {}, 종료 시간: {}", sessionId, endTime);

        // 학습 시간 계산 (초 단위)
        long studyTimeSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();
        if (studyTimeSeconds < 1) {
            studyTimeSeconds = 1; // 최소 1초
        }

        logger.info("학습 시간 계산 - 세션 ID: {}, 시작: {}, 종료: {}, 소요시간: {}초",
                sessionId, session.getStartTime(), endTime, studyTimeSeconds);

        // StudyRecord에 실시간 카운트를 기반으로 저장
        try {
            if (actualQuestions > 0) {
                saveStudyRecord(session.getUserId(), session.getLevelName(), session.getPartOfSpeech(),
                        actualQuestions, (int) studyTimeSeconds, convertStudyType(session.getStudyType()));
                logger.info("StudyRecord 저장 완료 - 사용자 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                        session.getUserId(), studyTimeSeconds, session.getCurrentCorrectCount(),
                        session.getCurrentIncorrectCount());
            }
        } catch (Exception e) {
            logger.error("StudyRecord 저장 실패 - 세션 ID: {}", sessionId, e);
        }
    }

    /**
     * 학습 세션 강제 종료 (뒤로가기 버튼용)
     */
    @Transactional
    public void endStudySessionById(Long sessionId) {
        logger.info("세션 강제 종료 요청 - 세션 ID: {}", sessionId);

        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("종료할 세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            return;
        }

        StudySession session = sessionOpt.get();

        // 이미 종료된 세션은 처리하지 않음
        if (session.getEndTime() != null) {
            logger.info("이미 종료된 세션 - 세션 ID: {}", sessionId);
            return;
        }

        // 현재까지의 학습 시간을 기록하고 세션 종료
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);

        // 실시간 카운트를 기반으로 총 문제 수와 정답 수 설정
        int actualQuestions = session.getTotalCurrentQuestions();
        session.setTotalQuestions(actualQuestions);
        session.setCorrectAnswers(session.getCurrentCorrectCount());

        studySessionRepository.save(session);

        // 학습 시간 계산 (초 단위)
        long studyTimeSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();
        if (studyTimeSeconds < 1) {
            studyTimeSeconds = 1; // 최소 1초
        }

        logger.info("세션 강제 종료 완료 - 세션 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                sessionId, studyTimeSeconds, session.getCurrentCorrectCount(),
                session.getCurrentIncorrectCount());

        // 학습한 문제가 있으면 StudyRecord에 저장
        if (actualQuestions > 0) {
            try {
                saveStudyRecord(session.getUserId(), session.getLevelName(), session.getPartOfSpeech(),
                        actualQuestions, (int) studyTimeSeconds,
                        convertStudyType(session.getStudyType()));
                logger.info("강제 종료 시 StudyRecord 저장 완료 - 사용자 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                        session.getUserId(), studyTimeSeconds, session.getCurrentCorrectCount(),
                        session.getCurrentIncorrectCount());
            } catch (Exception e) {
                logger.error("강제 종료 시 StudyRecord 저장 실패 - 세션 ID: {}", sessionId, e);
            }
        }
    }

    /**
     * 학습 기록 저장 (StudyRecord entity에 맞게 wordCount와 studyTime만 누적)
     */
    private void saveStudyRecord(Long userId, String levelName, String partOfSpeech,
                                 int wordCount, int studyTimeSeconds, StudyRecord.StudyType studyType) {
        LocalDate today = LocalDate.now();

        // 오늘 같은 조건의 기록이 있는지 확인
        List<StudyRecord> todayRecords = studyRecordRepository.findByUserIdAndDateRange(userId, today, today);

        Optional<StudyRecord> existingRecord = todayRecords.stream()
                .filter(record -> record.getLevelName().equals(levelName) &&
                        record.getPartOfSpeech().equals(partOfSpeech) &&
                        record.getStudyType().equals(studyType))
                .findFirst();

        if (existingRecord.isPresent()) {
            // 기존 기록에 누적
            StudyRecord record = existingRecord.get();
            record.setWordCount(record.getWordCount() + wordCount);
            record.setStudyTime(record.getStudyTime() + studyTimeSeconds); // 초 단위로 누적
            studyRecordRepository.save(record);
            logger.info("기존 학습 기록 업데이트 - 사용자: {}, 추가 시간: {}초, 총 시간: {}초",
                    userId, studyTimeSeconds, record.getStudyTime());
        } else {
            // 새로운 기록 생성
            StudyRecord newRecord = StudyRecord.builder()
                    .userId(userId)
                    .levelName(levelName)
                    .partOfSpeech(partOfSpeech)
                    .wordCount(wordCount)
                    .studyTime(studyTimeSeconds) // 초 단위로 저장
                    .studyDate(today)
                    .studyType(studyType)
                    .build();
            studyRecordRepository.save(newRecord);
            logger.info("새로운 학습 기록 생성 - 사용자: {}, 학습 시간: {}초", userId, studyTimeSeconds);
        }
    }


    /**
     * 사용자의 학습 기록 조회
     */
    public List<StudyRecordDto> getStudyRecords(Long userId) {
        List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

        return records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 단어별 학습 결과 기록
     */
    public void recordWordResult(Long userId, Long wordId, boolean isCorrect,
                                 WordStudyResult.StudyType studyType) {
        logger.debug("단어 학습 결과 기록 - 사용자: {}, 단어: {}, 정답 여부: {}, 타입: {}",
                userId, wordId, isCorrect, studyType);

        Optional<WordStudyResult> existingResult = wordStudyResultRepository
                .findByUserIdAndWordIdAndStudyType(userId, wordId, studyType);

        if (existingResult.isPresent()) {
            // 기존 기록 업데이트
            WordStudyResult result = existingResult.get();
            result.setIsCorrect(isCorrect);
            result.setAttemptCount(result.getAttemptCount() + 1);
            result.setLastStudiedAt(LocalDateTime.now());
            wordStudyResultRepository.save(result);
        } else {
            // 새로운 기록 생성
            WordStudyResult newResult = WordStudyResult.builder()
                    .userId(userId)
                    .wordId(wordId)
                    .studyType(studyType)
                    .isCorrect(isCorrect)
                    .attemptCount(1)
                    .lastStudiedAt(LocalDateTime.now())
                    .build();
            wordStudyResultRepository.save(newResult);
        }
    }

    /**
     * 틀린 단어 목록 조회
     */
    public List<WordDto> getIncorrectWords(Long userId, String levelName, String partOfSpeech,
                                           WordStudyResult.StudyType studyType) {
        logger.info("틀린 단어 조회 - 사용자: {}, 레벨: {}, 품사: {}, 타입: {}",
                userId, levelName, partOfSpeech, studyType);

        List<WordStudyResult> incorrectResults = wordStudyResultRepository
                .findIncorrectWordsByUserAndLevelAndPartAndType(userId, levelName, partOfSpeech, studyType);

        return incorrectResults.stream()
                .map(result -> {
                    Word word = wordRepository.findWordWithFormsById(result.getWordId());
                    return word != null ? new WordDto(word) : null;
                })
                .filter(wordDto -> wordDto != null)
                .collect(Collectors.toList());
    }

    /**
     * 틀린 단어 개수 조회
     */
    public Long getIncorrectWordCount(Long userId, String levelName, String partOfSpeech,
                                      WordStudyResult.StudyType studyType) {
        return wordStudyResultRepository.countIncorrectWordsByUserAndLevelAndPartAndType(
                userId, levelName, partOfSpeech, studyType);
    }

    /**
     * 총 학습 시간 조회 (초 단위)
     */
    public Integer getTotalStudyTimeInSeconds(Long userId, LocalDate startDate, LocalDate endDate) {
        Integer totalTime = studyRecordRepository.getTotalStudyTimeByUserIdAndDateRange(userId, startDate, endDate);
        return totalTime != null ? totalTime : 0;
    }

    /**
     * StudyRecord를 DTO로 변환
     */
    private StudyRecordDto convertToDto(StudyRecord record) {
        return StudyRecordDto.builder()
                .studyDate(record.getStudyDate().toString())
                .levelName(record.getLevelName())
                .partOfSpeech(record.getPartOfSpeech())
                .wordCount(record.getWordCount())
                .studyTime(record.getStudyTime()) // 초 단위
                .studyDuration(record.getFormattedStudyTime()) // 포맷팅된 시간 문자열
                .build();
    }

    private StudyRecord.StudyType convertStudyType(StudySession.StudyType studyType) {
        return StudyRecord.StudyType.valueOf(studyType.name());
    }

    /**
     * 전체 학습 통계 조회
     */
    public Map<String, Object> getOverviewStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 전체 학습한 단어 수
        Long totalStudiedWords = wordStudyResultRepository.countDistinctWordsByUserId(userId);

        // 전체 단어 수 (7150개 고정 또는 DB에서 조회)
        Long totalWords = 7150L;

        // 진도율 계산
        double progress = totalWords > 0 ? Math.round((double) totalStudiedWords / totalWords * 100 * 100.0) / 100.0 : 0.0;

        // 연속 학습 일수
        int consecutiveDays = calculateConsecutiveDays(userId);

        stats.put("totalWords", totalWords);
        stats.put("studiedWords", totalStudiedWords);
        stats.put("progress", progress);
        stats.put("consecutiveDays", consecutiveDays);

        return stats;
    }

    /**
     * 급수별 학습 통계 조회
     */
    public List<Map<String, Object>> getLevelStats(Long userId) {
        List<Map<String, Object>> levelStats = new ArrayList<>();

        // N5, N4, N3, N2, N1 순회
        String[] levels = {"N5", "N4", "N3", "N2", "N1"};
        Map<String, Integer> levelWordCounts = Map.of(
                "N5", 450, "N4", 700, "N3", 1000, "N2", 2000, "N1", 3000
        );

        for (String level : levels) {
            Map<String, Object> stat = new HashMap<>();

            // 해당 급수의 학습한 단어 수
            Long studiedWords = wordStudyResultRepository.countDistinctWordsByUserIdAndLevel(userId, level);

            // 전체 단어 수
            Integer totalWords = levelWordCounts.get(level);

            // 진도율 계산
            double progress = totalWords > 0 ? Math.round((double) studiedWords / totalWords * 100 * 100.0) / 100.0 : 0.0;

            stat.put("level", level);
            stat.put("totalWords", totalWords);
            stat.put("studiedWords", studiedWords);
            stat.put("progress", progress);

            levelStats.add(stat);
        }

        return levelStats;
    }

    /**
     * 품사별 학습 통계 조회
     */
    public List<Map<String, Object>> getPartStats(Long userId, String level) {
        List<Map<String, Object>> partStats = new ArrayList<>();

        String[] parts = {"명사", "형용사", "형용동사", "동사"};
        Map<String, Integer> partWordCounts = Map.of(
                "명사", 120, "형용사", 80, "형용동사", 60, "동사", 150
        );

        for (String part : parts) {
            Map<String, Object> stat = new HashMap<>();

            // 해당 품사의 학습한 단어 수
            Long studiedWords = wordStudyResultRepository.countDistinctWordsByUserIdAndLevelAndPart(userId, level, part);

            // 전체 단어 수
            Integer totalWords = partWordCounts.get(part);

            // 진도율 계산
            double progress = totalWords > 0 ? Math.round((double) studiedWords / totalWords * 100 * 100.0) / 100.0 : 0.0;

            stat.put("part", part);
            stat.put("totalWords", totalWords);
            stat.put("studiedWords", studiedWords);
            stat.put("progress", progress);

            partStats.add(stat);
        }

        return partStats;
    }

    /**
     * 연속 학습 일수 계산
     */
    private int calculateConsecutiveDays(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;
        int consecutiveDays = 0;

        while (true) {
            boolean studiedOnDate = studyRecordRepository.existsByUserIdAndStudyDate(userId, checkDate);

            if (studiedOnDate) {
                consecutiveDays++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }

        return consecutiveDays;
    }
}