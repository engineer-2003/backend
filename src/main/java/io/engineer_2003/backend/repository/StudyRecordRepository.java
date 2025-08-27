package io.engineer_2003.backend.repository;

import io.engineer_2003.backend.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
}
