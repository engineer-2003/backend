// src/main/java/com/engineer_2003/backend/service/StudyRecordService.java

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
import com.engineer_2003.backend.security.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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

        // 게스트 사용자(ID: 0L)도 DB에 세션을 저장하도록 변경
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
            studyTimeSeconds = 1; // 최소 1초
        }

        logger.info("학습 시간 계산 - 세션 ID: {}, 시작: {}, 종료: {}, 소요시간: {}초",
                sessionId, session.getStartTime(), endTime, studyTimeSeconds);

        // StudyRecord에 실시간 카운트를 기반으로 저장 (게스트 포함)
        try {
            if (actualQuestions > 0) {
                saveStudyRecord(session.getUserId(), session.getLevelName(), session.getPartOfSpeech(),
                        actualQuestions, (int) studyTimeSeconds, convertStudyType(session.getStudyType()));
                logger.info("StudyRecord 저장 완료 - 사용자 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                        session.getUserId(), studyTimeSeconds, session.getCurrentCorrectCount(),
                        session.getCurrentIncorrectCount());
            } else {
                logger.info("StudyRecord 저장 스킵: 문제 없음({})", actualQuestions);
            }
        } catch (Exception e) {
            logger.error("StudyRecord 저장 실패 - 세션 ID: {}", sessionId, e);
            // 외래 키 오류가 발생하면 여기에 잡힘
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
            studyTimeSeconds = 1; // 최소 1초
        }

        logger.info("세션 강제 종료 완료 - 세션 ID: {}, 학습 시간: {}초, 정답: {}, 오답: {}",
                sessionId, studyTimeSeconds, session.getCurrentCorrectCount(),
                session.getCurrentIncorrectCount());

        // 학습한 문제가 있으면 StudyRecord에 저장 (게스트 포함)
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
                // 외래 키 오류가 발생하면 여기에 잡힘
            }
        } else {
            logger.info("강제 종료 시 StudyRecord 저장 스킵: 문제 없음({})", actualQuestions);
        }
    }

    /**
     * 학습 기록 저장 (StudyRecord entity에 맞게 wordCount와 studyTime만 누적)
     */
    private void saveStudyRecord(Long userId, String levelName, String partOfSpeech,
                                 int wordCount, int studyTimeSeconds, StudyRecord.StudyType studyType) {
        // userId가 0L인 경우에도 저장 진행 (DB에서 FK 제약 조건 제거 필수)
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
        // userId가 0L인 경우에도 조회 진행
        List<StudyRecord> records = studyRecordRepository.findByUserIdOrderByStudyDateDesc(userId);

        return records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 단어별 학습 결과 기록 (게스트 포함)
     */
    public void recordWordResult(Long userId, Long wordId, boolean isCorrect,
                                 WordStudyResult.StudyType studyType) {
        // userId가 0L인 경우에도 저장 진행 (DB에서 FK 제약 조건 제거 필수)
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
     * 틀린 단어 목록 조회 (게스트 포함)
     */
    public List<WordDto> getIncorrectWords(Long userId, String levelName, String partOfSpeech,
                                           WordStudyResult.StudyType studyType) {
        // userId가 0L인 경우에도 조회 진행
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
     * 틀린 단어 개수 조회 (게스트 포함)
     */
    public Long getIncorrectWordCount(Long userId, String levelName, String partOfSpeech,
                                      WordStudyResult.StudyType studyType) {
        // userId가 0L인 경우에도 조회 진행
        return wordStudyResultRepository.countIncorrectWordsByUserAndLevelAndPartAndType(
                userId, levelName, partOfSpeech, studyType);
    }

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