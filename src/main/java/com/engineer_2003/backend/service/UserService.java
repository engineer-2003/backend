package com.engineer_2003.backend.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.engineer_2003.backend.dto.UserRegistrationDto;
import com.engineer_2003.backend.entity.User; // DTO 임포트
import com.engineer_2003.backend.repository.UserRepository;
import com.engineer_2003.backend.security.jwt.JwtTokenProvider;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    // 회원가입
    @Transactional
    public void registerUser(UserRegistrationDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword())) // 비밀번호 암호화
                .email(userDto.getEmail()) // 이메일 필드 추가
                .build();

        userRepository.save(user);
    }

    // 비밀번호 찾기 기능 (토큰 발송)
    @Transactional
    public void findPassword(String username, String email) {
        User user = userRepository.findByUsernameAndEmail(username, email);
        if (user == null) {
            throw new IllegalArgumentException("아이디 또는 이메일이 잘못되었습니다.");
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        userRepository.save(user);

        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    // 비밀번호 재설정 기능 (새 비밀번호 저장)
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1. 토큰으로 사용자 찾기
        User user = userRepository.findByResetToken(token);
        if (user == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        // 2. 비밀번호 재설정
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null); // 사용된 토큰은 무효화
        userRepository.save(user);
    }
    
    // 로그인
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) { // 암호화된 비밀번호 대조
            return jwtTokenProvider.generateToken(username); // JWT 토큰 생성
        }
        return null;
    }

    // 사용자명으로 사용자 조회
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // 사용자명으로 사용자 ID 조회
    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username);
        }
        return user.getUserId(); // User 엔티티의 ID getter 메서드
    }
}