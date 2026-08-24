package com.apitest.ddulo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				// CSRF 보안 비활성화 (API 서버는 보통 끔)
				.csrf(AbstractHttpConfigurer::disable)

				// 기본 로그인 화면 비활성화
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)

				// 인증/인가 설정 (모두 허용)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/error").permitAll()
						.anyRequest().permitAll()
				)

				// 세션 설정 (Stateless, 메모리 절약)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)

				.build();
	}
}