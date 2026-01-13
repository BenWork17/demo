package com.baohoanhao.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * State Service for OAuth2 CSRF Protection
 * 
 * Stores and validates state parameters used in OAuth2 flows to prevent CSRF attacks.
 * State parameters are stored in Redis with a short TTL (5 minutes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("oauth")
@ConditionalOnProperty(value = "app.oauth.enabled", havingValue = "true")
public class StateService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String STATE_PREFIX = "oauth_state:";
    private static final long STATE_TTL_MINUTES = 5;
    
    /**
     * Store OAuth state with associated redirect URL
     * 
     * @param state Random state parameter
     * @param redirectUrl URL to redirect after OAuth completion
     */
    public void storeState(String state, String redirectUrl) {
        String key = STATE_PREFIX + state;
        redisTemplate.opsForValue().set(
            key,
            redirectUrl,
            STATE_TTL_MINUTES,
            TimeUnit.MINUTES
        );
        log.debug("Stored OAuth state: {} with redirect URL: {}", state, redirectUrl);
    }
    
    /**
     * Validate state and retrieve associated redirect URL
     * State is deleted after validation (one-time use)
     * 
     * @param state State parameter to validate
     * @return Redirect URL if state is valid, null otherwise
     */
    public String validateAndGetRedirectUrl(String state) {
        if (state == null || state.isBlank()) {
            log.warn("Empty state parameter provided");
            return null;
        }
        
        String key = STATE_PREFIX + state;
        Object redirectUrl = redisTemplate.opsForValue().get(key);
        
        if (redirectUrl == null) {
            log.warn("Invalid or expired state parameter: {}", state);
            return null;
        }
        
        // Delete state after validation (one-time use)
        redisTemplate.delete(key);
        log.debug("Validated and consumed OAuth state: {}", state);
        
        return redirectUrl.toString();
    }
    
    /**
     * Clean up expired states (optional - Redis TTL handles this automatically)
     */
    public void cleanupExpiredStates() {
        // Redis automatically removes expired keys
        // This method is here for manual cleanup if needed
        log.debug("Expired states are automatically cleaned by Redis TTL");
    }
}
