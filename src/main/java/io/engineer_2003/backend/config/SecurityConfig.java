//테스트용 파일 

package io.engineer_2003.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (REST API 테스트용)
            .csrf(csrf -> csrf.disable())

            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()  // API 요청은 누구나 접근 가능
                .anyRequest().permitAll()                // 그 외 요청도 전부 허용
            );

        return http.build();
    }
}
