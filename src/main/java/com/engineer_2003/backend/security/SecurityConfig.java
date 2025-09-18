package com.engineer_2003.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.engineer_2003.backend.security.jwt.JwtAuthenticationFilter;
import com.engineer_2003.backend.security.jwt.JwtTokenProvider;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 아래 엔드포인트는 인증 없이 접근을 허용합니다.
                        .requestMatchers(
                            "/api/users/signup", 
                            "/api/users/login", 
                            "/api/users/find-password",
                            "/api/users/reset-password" 
                        ).permitAll()

                        // 인증 없이 접근 가능한 학습 API (단어 조회, 문제 생성 등)
                        .requestMatchers("/study", "/study/{id}", "/study/{id}/sentence-problem", "/study/{id}/form-problem", "/study/{id}/check").permitAll()

                        // 인증이 필요한 사용자별 데이터 API
                        .requestMatchers("/study/incorrect/**", "/study/session/**", "/study/records", "/study/result").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}