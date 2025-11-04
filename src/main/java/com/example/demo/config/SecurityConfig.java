package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 간편 테스트용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",  // 헬스체크는 열어두기
                    "/css/**", "/js/**", "/images/**", "/static/**"
                ).permitAll()
                .anyRequest().permitAll()                // ★ 전체 허용 (로그인 화면 사라짐)
            )
            .formLogin(form -> form.disable())          // 폼 로그인 비활성화
            .httpBasic(basic -> basic.disable());       // HTTP Basic 비활성화
        return http.build();
    }
}
