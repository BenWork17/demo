package com.baohoanhao.demo.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token Storage Service - Quản lý tokens trong Redis
 * 
 * Chức năng:
 * - Lưu refresh token để tracking và revoke
 * - Blacklist access token khi logout
 * - Kiểm tra token có bị revoke không
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenStorageService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key prefixes
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    /**
     * Lưu refresh token vào Redis
     * Key pattern: refresh_token:{userId}:{tokenHash}
     */
    public void storeRefreshToken(String userId, String refreshToken, long ttlMillis) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Stored refresh token for user: {}", userId);
    }

    /**
     * Lấy refresh token của user
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * Xóa refresh token (khi logout hoặc refresh)
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for user: {}", userId);
    }

    /**
     * Blacklist access token khi logout
     * Token sẽ tự động bị xóa khi hết hạn (TTL)
     */
    public void blacklistToken(String token, long ttlMillis) {
        String key = BLACKLIST_PREFIX + hashToken(token);
        redisTemplate.opsForValue().set(key, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Blacklisted token");
    }

    /**
     * Kiểm tra token có bị blacklist không
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + hashToken(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Revoke tất cả tokens của user (force logout everywhere)
     */
    public void revokeAllUserTokens(String userId) {
        // Xóa refresh token
        deleteRefreshToken(userId);
        
        // Có thể thêm logic blacklist các access tokens đang active
        // Tuy nhiên cách tốt hơn là dùng token version trong DB
        log.info("Revoked all tokens for user: {}", userId);
    }

    /**
     * Hash token để làm key (không lưu token raw vào key)
     */
    private String hashToken(String token) {
        // Lấy 32 ký tự cuối của token làm identifier
        // Đủ unique và không expose toàn bộ token
        return token.length() > 32 ? token.substring(token.length() - 32) : token;
    }
}
