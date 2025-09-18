// StudySessionRepository.java
package com.engineer_2003.backend.repository;

import com.engineer_2003.backend.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserIdOrderByStartTimeDesc(Long userId);

    Optional<StudySession> findByUserIdAndEndTimeIsNull(Long userId);
}