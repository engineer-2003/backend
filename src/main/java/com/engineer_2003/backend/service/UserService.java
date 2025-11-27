// src/main/java/com/engineer_2003/backend/service/UserService.java
package com.engineer_2003.backend.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.engineer_2003.backend.dto.UserRegistrationDto;
import com.engineer_2003.backend.entity.User;
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

    //아이디 중복 확인
    public boolean checkUsernameDuplication(String username) {
        return userRepository.existsByUsername(username);
    }
    
    // 이메일 중복 확인
    
    public boolean checkEmailDuplication(String email) {
        return userRepository.existsByEmail(email);
    }

    // 회원가입
    @Transactional
    public void registerUser(UserRegistrationDto userDto) {
        if (checkUsernameDuplication(userDto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (checkEmailDuplication(userDto.getEmail())) { // 이메일 중복 체크
             throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword())) // 비밀번호 암호화
                .email(userDto.getEmail()) // 이메일 필드 추가
                .build();

        userRepository.save(user);
    }

    // Google 로그인 처리
    @Transactional
    public String processGoogleLogin(String email) {
        User user = userRepository.findByEmailAndProvider(email, "google");
        // ... (생략)
        
        if (user == null) {
            // 구글 정보로 신규 회원 생성
            // username은 유니크해야 하므로 이메일 앞부분과 랜덤 문자열을 조합
            String username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);

            // 이미 해당 username이 존재할 경우를 대비한 예외 처리 
            while (userRepository.existsByUsername(username)) {
                username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
            }

            User newUser = User.builder()
                    .username(username)
                    .email(email)
                    // 소셜 로그인이므로 비밀번호는 랜덤 값으로 채움
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .provider("google")
                    .build();
            userRepository.save(newUser);
            user = newUser;
        }

        // JWT 토큰 생성 및 반환
        return jwtTokenProvider.generateToken(user.getUsername());
    }

    // 비밀번호 찾기 기능 (토큰 발송)
    @Transactional
    public void findPassword(String username, String email) {
        // ... (생략)
        User user = userRepository.findByUsernameAndEmail(username, email);
        if (user == null) {
            throw new IllegalArgumentException("아이디 또는 이메일이 잘못되었습니다.");
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        userRepository.save(user);
        
        String resetLink = "https://japanese-haru-frontend.onrender.com/user/reset-password?token=" + resetToken;  // 프론트엔드 경로 업데이트
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    // 비밀번호 재설정 기능 (새 비밀번호 저장)
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // ... (생략)
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
        // ... (생략)
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

    // 사용자명으로 사용자 ID 조회 (AuthUtils와 별개로 내부 서비스에서 사용)
    public Long getUserIdByUsername(String username) {
        // ... (생략)
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username);
        }
        return user.getUserId(); // User 엔티티의 ID getter 메서드
    }
}