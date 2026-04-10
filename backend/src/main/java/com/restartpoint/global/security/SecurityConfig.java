package com.restartpoint.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        // 공개 API
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seasons/**").permitAll()
                        // 팀 프로젝트 조회는 인증 필요 (팀 목록/상세는 공개)
                        .requestMatchers(HttpMethod.GET, "/api/v1/teams/*/project").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/teams/**").permitAll()
                        // 커뮤니티 게시글 조회는 공개
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/**").permitAll()
                        // 프로젝트/체크포인트는 팀원만 접근 가능 (인증 필요)
                        // Actuator (헬스체크)
                        .requestMatchers("/actuator/**").permitAll()
                        // H2 콘솔
                        .requestMatchers("/h2-console/**").permitAll()
                        // 관리자 전용
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/growth-reports/generate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/seasons/*/growth-reports/generate").hasRole("ADMIN")
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 콘솔용

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
