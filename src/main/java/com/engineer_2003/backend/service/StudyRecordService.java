package com.engineer_2003.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.engineer_2003.backend.dto.StudyRecordResponseDto;
import com.engineer_2003.backend.entity.StudyRecord;
import com.engineer_2003.backend.repository.StudyRecordRepository;
import com.engineer_2003.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyRecordService {

    private final StudyRecordRepository studyRecordRepository;
    private final UserRepository userRepository;

    public List<StudyRecord> getRecordsByUser(Long userId) {
        return studyRecordRepository.findByUserUserId(userId);
    }
    
    //  DTO 변환 메소드 (사용자 이름으로 조회)
    public List<StudyRecordResponseDto> getRecordsDtoByUser(String username) {
        // 사용자 이름으로 User 엔티티를 찾고 userId를 얻습니다.
        Long userId = userRepository.findByUsername(username).getUserId();
        
        return studyRecordRepository.findByUserUserId(userId).stream()
                .map(r -> new StudyRecordResponseDto(
                        r.getStudyDate(),
                        r.getLevelName(),
                        r.getPartOfSpeech(),
                        r.getWordCount(),
                        r.getStudyTime()
                ))
                .collect(Collectors.toList());
    }
}