package com.baohoanhao.demo.config;

import com.baohoanhao.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration
 * 
 * JWT-based stateless authentication:
 * - Không dùng session (STATELESS)
 * - Tắt CSRF (vì không có session)
 * - JWT filter validate token trước mỗi request
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Cho phép dùng @PreAuthorize trên method
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Các endpoints công khai không cần authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/auth/oauth2/**",
            "/api/public/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF - Không cần vì dùng JWT (stateless)
                .csrf(AbstractHttpConfigurer::disable)
                
                // 2. Bật CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 3. Session Management - STATELESS cho JWT
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 4. Cấu hình Authorization
                .authorizeHttpRequests(auth -> auth
                        // Cho phép OPTIONS (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Public endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        
                        // Tất cả request khác cần authentication
                        .anyRequest().authenticated()
                )
                
                // 5. Thêm JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Cho phép frontend origins (production nên config cụ thể)
        config.setAllowedOriginPatterns(List.of("*"));
        
        // Các HTTP methods được phép
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Các headers được phép
        config.setAllowedHeaders(List.of("*"));
        
        // Headers expose cho frontend đọc
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Total-Count"));
        
        // Cho phép gửi credentials (cookies, authorization headers)
        config.setAllowCredentials(true);
        
        // Cache preflight response
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}