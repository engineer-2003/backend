package com.engineer_2003.backend.security;

import com.engineer_2003.backend.entity.User;
import com.engineer_2003.backend.repository.UserRepository;
import com.engineer_2003.backend.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthUtils(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (Authorization 헤더 사용)
     */
    public Long extractUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 Authorization 헤더입니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거
        return extractUserIdFromToken(token);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public Long extractUserIdFromToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
        }

        String username = getUsernameFromToken(token);
        return getUserIdByUsername(username);
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            return auth.getName();
        } catch (Exception e) {
            throw new IllegalArgumentException("토큰에서 사용자명을 추출할 수 없습니다.", e);
        }
    }

    /**
     * username으로 사용자 ID 조회
     */
    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username);
        }
        return user.getUserId();
    }
}