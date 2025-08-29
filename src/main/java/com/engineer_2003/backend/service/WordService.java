// WordService.java
package com.engineer_2003.backend.service;

import com.engineer_2003.backend.dto.WordDto;
import com.engineer_2003.backend.entity.Word;
import com.engineer_2003.backend.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@Transactional
public class WordService {

    private static final Logger logger = LoggerFactory.getLogger(WordService.class);

    @Autowired
    private WordRepository wordRepository;

    public List<WordDto> getWordsByLevelAndPart(String levelName, String partOfSpeech) {
        logger.debug("단어 조회 시작 - 레벨: {}, 품사: {}", levelName, partOfSpeech);

        try {
            List<Word> words = wordRepository.findWordsWithFormsbyLevelAndPart(levelName, partOfSpeech);
            logger.debug("DB에서 조회된 단어 수: {}", words.size());

            List<WordDto> wordDtos = words.stream()
                    .map(word -> {
                        try {
                            return new WordDto(word);
                        } catch (Exception e) {
                            logger.warn("WordDto 변환 실패 - 단어 ID: {}", word.getId(), e);
                            return null;
                        }
                    })
                    .filter(dto -> dto != null) // null인 항목 제거
                    // ------ 이 부분 수정 --------
                    // 품사가 명사가 아니고, 활용형이 있어야 하는 경우에만 활용형 필터를 적용
                    .filter(dto -> {
                        if (("동사".equals(partOfSpeech) || "형용사".equals(partOfSpeech))) {
                            // 동사 또는 형용사일 경우 활용형이 비어있으면 필터링
                            return dto.getForms() != null && !dto.getForms().isEmpty();
                        } else {
                            // 그 외 품사 (명사 등)는 활용형 필터 적용 없이 통과
                            return true;
                        }
                    })
                    // -----------------------------
                    .collect(Collectors.toList());

            logger.debug("변환된 WordDto 수: {}", wordDtos.size());

            // 랜덤하게 섞기
            Collections.shuffle(wordDtos);

            logger.info("단어 목록 조회 완료 - 레벨: {}, 품사: {}, 반환 개수: {}",
                    levelName, partOfSpeech, wordDtos.size());

            return wordDtos;

        } catch (Exception e) {
            logger.error("단어 목록 조회 중 오류 발생 - 레벨: {}, 품사: {}", levelName, partOfSpeech, e);
            throw new RuntimeException("단어 목록 조회 실패", e);
        }
    }

    public WordDto getWordById(Long id) {
        logger.debug("단어 조회 - ID: {}", id);

        try {
            Word word = wordRepository.findWordWithFormsById(id);

            if (word == null) {
                logger.info("단어를 찾을 수 없음 - ID: {}", id);
                return null;
            }

            WordDto wordDto = new WordDto(word);
            logger.debug("단어 조회 성공 - ID: {}, 단어: {}", id, word.getWord());

            return wordDto;

        } catch (Exception e) {
            logger.error("단어 조회 중 오류 발생 - ID: {}", id, e);
            throw new RuntimeException("단어 조회 실패", e);
        }
    }

    public boolean checkAnswer(Long wordId, String answer, String formType) {
        logger.debug("정답 확인 - 단어 ID: {}, 활용형: {}, 답안: {}", wordId, formType, answer);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return false;
            }

            WordDto wordDto = new WordDto(word);

            // 기본 단어 확인 (원형)
            if ("word".equals(formType) || "원형".equals(formType)) {
                boolean isCorrect = word.getWord().equals(answer) || word.getReading().equals(answer);
                logger.debug("원형 정답 확인 결과: {} (단어: {}, 읽기: {}, 답안: {})",
                        isCorrect, word.getWord(), word.getReading(), answer);
                return isCorrect;
            }

            // 활용형 확인
            if (wordDto.getForms().containsKey(formType)) {
                String correctAnswer = wordDto.getForms().get(formType);
                boolean isCorrect = correctAnswer != null && correctAnswer.equals(answer);

                logger.debug("활용형 정답 확인 결과: {} (정답: {}, 답안: {})",
                        isCorrect, correctAnswer, answer);
                return isCorrect;
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})", formType, wordId);
            return false;

        } catch (Exception e) {
            logger.error("정답 확인 중 오류 발생 - 단어 ID: {}", wordId, e);
            return false;
        }
    }

    /**
     * 정답을 반환하는 메서드 (힌트나 정답 표시용)
     */
    public String getCorrectAnswer(Long wordId, String formType) {
        logger.debug("정답 조회 - 단어 ID: {}, 활용형: {}", wordId, formType);

        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                logger.warn("단어를 찾을 수 없음 - ID: {}", wordId);
                return "";
            }

            WordDto wordDto = new WordDto(word);

            // 기본 단어 반환
            if ("word".equals(formType) || "원형".equals(formType)) {
                return word.getWord();
            }

            // 활용형 반환
            if (wordDto.getForms().containsKey(formType)) {
                return wordDto.getForms().get(formType);
            }

            logger.warn("지원하지 않는 활용형 타입: {} (단어 ID: {})", formType, wordId);
            return "";

        } catch (Exception e) {
            logger.error("정답 조회 중 오류 발생 - 단어 ID: {}", wordId, e);
            return "";
        }
    }

    /**
     * 특정 단어의 활용형 목록 조회
     */
    public List<String> getAvailableForms(Long wordId) {
        try {
            Word word = wordRepository.findWordWithFormsById(wordId);
            if (word == null) {
                return Collections.emptyList();
            }

            WordDto wordDto = new WordDto(word);
            return wordDto.getForms().keySet().stream()
                    .filter(key -> wordDto.getForms().get(key) != null && !wordDto.getForms().get(key).isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("활용형 목록 조회 중 오류 발생 - 단어 ID: {}", wordId, e);
            return Collections.emptyList();
        }
    }
}