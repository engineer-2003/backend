// src/main/java/com/engineer_2003/backend/repository/UserRepository.java
package com.engineer_2003.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.engineer_2003.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    User findByUsername(String username);
    User findByUsernameAndEmail(String username, String email);
    User findByResetToken(String resetToken); 
    User findByEmailAndProvider(String email, String provider); 
}