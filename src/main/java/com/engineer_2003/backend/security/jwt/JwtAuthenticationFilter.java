// JwtAuthenticationFilter.java
package com.engineer_2003.backend.security.jwt;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        String requestUri = request.getRequestURI();

        if (token == null) {
            System.out.println("[JWT] 토큰 없음: " + requestUri);

        } else if (jwtTokenProvider.validateToken(token)) {
            SecurityContextHolder.getContext()
                    .setAuthentication(jwtTokenProvider.getAuthentication(token));

            System.out.println("[JWT] 인증 성공: " + requestUri);

        } else {
            System.out.println("[JWT] 토큰 검증 실패: " + requestUri);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
