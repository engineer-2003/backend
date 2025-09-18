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
                .userId(userId)
                .levelName(levelName)
                .partOfSpeech(partOfSpeech)
                .studyType(studyType)
                .isRetrySession(isRetrySession)
                .startTime(LocalDateTime.now())
                .build();

        return studySessionRepository.save(session);
    }

    /**
     * 학습 세션 종료 및 기록 저장
     */
    public void endStudySession(Long sessionId, int totalQuestions, int correctAnswers) {
        logger.info("학습 세션 종료 - 세션 ID: {}, 총 문제: {}, 정답: {}", sessionId, totalQuestions, correctAnswers);

        Optional<StudySession> sessionOpt = studySessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            logger.warn("세션을 찾을 수 없음 - 세션 ID: {}", sessionId);
            return;
        }

        StudySession session = sessionOpt.get();
        session.setEndTime(LocalDateTime.now());
        session.setTotalQuestions(totalQuestions);
        session.setCorrectAnswers(correctAnswers);

        // 학습 시간 계산 (분 단위)
        long studyTimeMinutes = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        if (studyTimeMinutes < 1) {
            studyTimeMinutes = 1; // 최소 1분
        }

        studySessionRepository.save(session);

        // StudyRecord에 저장
        saveStudyRecord(session.getUserId(), session.getLevelName(), session.getPartOfSpeech(),
                totalQuestions, (int) studyTimeMinutes, convertStudyType(session.getStudyType()));
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
     * 학습 기록 저장
     */
    private void saveStudyRecord(Long userId, String levelName, String partOfSpeech,
                                 int wordCount, int studyTime, StudyRecord.StudyType studyType) {
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
            record.setStudyTime(record.getStudyTime() + studyTime);
            studyRecordRepository.save(record);
        } else {
            // 새로운 기록 생성
            StudyRecord newRecord = StudyRecord.builder()
                    .userId(userId)
                    .levelName(levelName)
                    .partOfSpeech(partOfSpeech)
                    .wordCount(wordCount)
                    .studyTime(studyTime)
                    .studyDate(today)
                    .studyType(studyType)
                    .build();
            studyRecordRepository.save(newRecord);
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

    private StudyRecordDto convertToDto(StudyRecord record) {
        return StudyRecordDto.builder()
                .studyDate(record.getStudyDate().toString())
                .levelName(record.getLevelName())
                .partOfSpeech(record.getPartOfSpeech())
                .wordCount(record.getWordCount())
                .studyTime(record.getStudyTime())
                .build();
    }

    private StudyRecord.StudyType convertStudyType(StudySession.StudyType studyType) {
        return StudyRecord.StudyType.valueOf(studyType.name());
    }
}