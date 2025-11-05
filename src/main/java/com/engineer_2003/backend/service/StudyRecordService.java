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
import com.engineer_2003.backend.security.AuthUtils; // AuthUtils 추가
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections; // Collections 추가
import java.util.List;
import java.util.Optional;
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
                .userId(userId) // 게스트(0L)도 저장
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

    // ... (updateSessionCount, endStudySession, endStudySessionById, saveStudyRecord, getStudyRecords는 동일하게 유지하거나 StudyController에서 이미 처리)

    // updateSessionCount: 변경 없음
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

    // endStudySession: 변경 없음 (세션의 userId를 사용하여 StudyRecord 저장)
    @Transactional
    public void endStudySession(Long sessionId, int totalQuestions, int correctAnswers) {
        logger.info("학습 세션 종료 - 세션 ID: {}, 총 문제: {}, 정답: {}", sessionId, totalQuestions, correctAnswers);

        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }

        StudySession session = sessionOpt.get();

        if (session.getEndTime() != null) {
            logger.warn("이미 종료된 세션 - 세션 ID: {}, 기존 종료 시간: {}", sessionId, session.getEndTime());
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);

        int actualQuestions = session.getTotalCurrentQuestions();
        int actualCorrect = session.getCurrentCorrectCount();

        session.setTotalQuestions(actualQuestions);
        session.setCorrectAnswers(actualCorrect);

        studySessionRepository.save(session);
        logger.info("세션 종료 시간 설정 완료 - 세션 ID: {}, 종료 시간: {}", sessionId, endTime);

        long studyTimeSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();
        if (studyTimeSeconds < 1) {
            studyTimeSeconds = 1;
        }

        logger.info("학습 시간 계산 - 세션 ID: {}, 시작: {}, 종료: {}, 소요시간: {}초",
                sessionId, session.getStartTime(), endTime, studyTimeSeconds);

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

    // endStudySessionById: 변경 없음 (세션의 userId를 사용하여 StudyRecord 저장)
    @Transactional
    public void endStudySessionById(Long sessionId) {
        logger.info("세션 강제 종료 요청 - 세션 ID: {}", sessionId);

        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("종료할 세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            return;
        }

        StudySession session = sessionOpt.get();

        if (session.getEndTime() != null) {
            logger.info("이미 종료된 세션 - 세션 ID: {}", sessionId);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);

        int actualQuestions = session.getTotalCurrentQuestions();
        session.setTotalQuestions(actualQuestions);
        session.setCorrectAnswers(session.getCurrentCorrectCount());

        studySessionRepository.save(session);

        long studyTimeSeconds = java.time.Duration.between(session.getStartTime(), endTime).getSeconds();
        if (studyTimeSeconds < 1) {
            studyTimeSeconds = 1;
        }

        logger.info("세션 강제 종료 완료 - 세션 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                sessionId, studyTimeSeconds, session.getCurrentCorrectCount(),
                session.getCurrentIncorrectCount());

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

    // saveStudyRecord: 변경 없음 (userId=0L인 경우 게스트 학습 기록으로 저장됨)
    private void saveStudyRecord(Long userId, String levelName, String partOfSpeech,
                                 int wordCount, int studyTimeSeconds, StudyRecord.StudyType studyType) {
        LocalDate today = LocalDate.now();

        List<StudyRecord> todayRecords = studyRecordRepository.findByUserIdAndDateRange(userId, today, today);

        Optional<StudyRecord> existingRecord = todayRecords.stream()
                .filter(record -> record.getLevelName().equals(levelName) &&
                        record.getPartOfSpeech().equals(partOfSpeech) &&
                        record.getStudyType().equals(studyType))
                .findFirst();

        if (existingRecord.isPresent()) {
            StudyRecord record = existingRecord.get();
            record.setWordCount(record.getWordCount() + wordCount);
            record.setStudyTime(record.getStudyTime() + studyTimeSeconds);
            studyRecordRepository.save(record);
            logger.info("기존 학습 기록 업데이트 - 사용자: {}, 추가 시간: {}초, 총 시간: {}초",
                    userId, studyTimeSeconds, record.getStudyTime());
        } else {
            StudyRecord newRecord = StudyRecord.builder()
                    .userId(userId)
                    .levelName(levelName)
                    .partOfSpeech(partOfSpeech)
                    .wordCount(wordCount)
                    .studyTime(studyTimeSeconds)
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
        // StudyController에서 userId=0L일 때 빈 목록을 반환하도록 처리
        List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

        return records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 단어별 학습 결과 기록 (게스트는 저장 스킵)
     */
    public void recordWordResult(Long userId, Long wordId, boolean isCorrect,
                                 WordStudyResult.StudyType studyType) {
        if (userId == AuthUtils.GUEST_USER_ID) {
            logger.warn("게스트 사용자는 단어별 학습 결과를 저장할 수 없습니다. 스킵합니다.");
            return;
        }

        logger.debug("단어 학습 결과 기록 - 사용자: {}, 단어: {}, 정답 여부: {}, 타입: {}",
                userId, wordId, isCorrect, studyType);

        Optional<WordStudyResult> existingResult = wordStudyResultRepository
                .findByUserIdAndWordIdAndStudyType(userId, wordId, studyType);

        if (existingResult.isPresent()) {
            WordStudyResult result = existingResult.get();
            result.setIsCorrect(isCorrect);
            result.setAttemptCount(result.getAttemptCount() + 1);
            result.setLastStudiedAt(LocalDateTime.now());
            wordStudyResultRepository.save(result);
        } else {
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
     * 틀린 단어 목록 조회 (게스트는 조회 스킵)
     */
    public List<WordDto> getIncorrectWords(Long userId, String levelName, String partOfSpeech,
                                           WordStudyResult.StudyType studyType) {
        if (userId == AuthUtils.GUEST_USER_ID) {
            logger.warn("게스트 사용자는 틀린 단어 목록을 조회할 수 없습니다. 빈 목록을 반환합니다.");
            return Collections.emptyList();
        }

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
     * 틀린 단어 개수 조회 (게스트는 조회 스킵)
     */
    public Long getIncorrectWordCount(Long userId, String levelName, String partOfSpeech,
                                      WordStudyResult.StudyType studyType) {
        if (userId == AuthUtils.GUEST_USER_ID) {
            return 0L;
        }

        return wordStudyResultRepository.countIncorrectWordsByUserAndLevelAndPartAndType(
                userId, levelName, partOfSpeech, studyType);
    }

    // ... (나머지 메서드는 변경 없음)
    public Integer getTotalStudyTimeInSeconds(Long userId, LocalDate startDate, LocalDate endDate) {
        Integer totalTime = studyRecordRepository.getTotalStudyTimeByUserIdAndDateRange(userId, startDate, endDate);
        return totalTime != null ? totalTime : 0;
    }

    private StudyRecordDto convertToDto(StudyRecord record) {
        return StudyRecordDto.builder()
                .studyDate(record.getStudyDate().toString())
                .levelName(record.getLevelName())
                .partOfSpeech(record.getPartOfSpeech())
                .wordCount(record.getWordCount())
                .studyTime(record.getStudyTime())
                .studyDuration(record.getFormattedStudyTime())
                .build();
    }

    private StudyRecord.StudyType convertStudyType(StudySession.StudyType studyType) {
        return StudyRecord.StudyType.valueOf(studyType.name());
    }
}