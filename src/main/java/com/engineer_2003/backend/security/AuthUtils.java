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

    // 게스트 사용자 ID 정의
    public static final Long GUEST_USER_ID = 0L;
    public static final String GUEST_USERNAME = "guest_user";


    public AuthUtils(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (Authorization 헤더 사용)
     * 토큰이 없거나 유효하지 않으면 GUEST_USER_ID(0L)를 반환
     */
    public Long extractUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return GUEST_USER_ID;
        }

        String token = authHeader.substring(7); // "Bearer " 제거
        try {
            return extractUserIdFromToken(token);
        } catch (IllegalArgumentException e) {
            // 토큰이 유효하지 않으면 게스트로 처리
            return GUEST_USER_ID;
        }
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