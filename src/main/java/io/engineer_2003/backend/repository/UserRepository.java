package io.engineer_2003.backend.repository;

import io.engineer_2003.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// <User, Long> → User 엔티티, PK 타입(Long)
public interface UserRepository extends JpaRepository<User, Long> {
    // username으로 유저 찾기 (자동 쿼리 생성됨)
    User findByUsername(String username);
}
