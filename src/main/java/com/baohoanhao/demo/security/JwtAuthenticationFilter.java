package com.baohoanhao.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * Filter này chạy trước mọi request để:
 * 1. Extract JWT từ Authorization header
 * 2. Validate token
 * 3. Set Authentication vào SecurityContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. Lấy Authorization header
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // 2. Kiểm tra có Bearer token không
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token
        final String jwt = authHeader.substring(BEARER_PREFIX.length());
        
        try {
            // 4. Validate token
            if (!jwtService.isTokenValid(jwt)) {
                log.debug("Invalid JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Kiểm tra token type phải là "access"
            String tokenType = jwtService.extractTokenType(jwt);
            if (!"access".equals(tokenType)) {
                log.debug("Not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            // 6. Kiểm tra token có bị blacklist không
            if (tokenStorageService.isTokenBlacklisted(jwt)) {
                log.debug("Token is blacklisted");
                filterChain.doFilter(request, response);
                return;
            }

            // 7. Nếu chưa có Authentication trong context
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String userId = jwtService.extractUserId(jwt);
                String email = jwtService.extractEmail(jwt);
                String role = jwtService.extractRole(jwt);

                // 8. Tạo Authentication với role
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,  // Principal = userId
                        null,    // Credentials = null (đã validate bằng JWT)
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 9. Set vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                log.debug("Authenticated user: {} with role: {}", email, role);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
