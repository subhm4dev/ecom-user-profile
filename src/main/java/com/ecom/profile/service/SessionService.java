package com.ecom.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Session Service
 * 
 * <p>Handles token blacklisting using Redis.
 * This is a blocking (MVC) version, not reactive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Check if token is blacklisted
     * 
     * @param tokenId Token ID (jti) from JWT
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        String key = BLACKLIST_PREFIX + tokenId;
        String value = redisTemplate.opsForValue().get(key);
        
        boolean blacklisted = value != null;
        
        if (blacklisted) {
            log.debug("Token is blacklisted: tokenId={}", tokenId);
        }
        
        return blacklisted;
    }

    /**
     * Blacklist a token (used during logout)
     * 
     * @param tokenId Token ID (jti)
     * @param expirySeconds Expiry time in seconds (from JWT expiry)
     */
    public void blacklistToken(String tokenId, long expirySeconds) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }

        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", expirySeconds, TimeUnit.SECONDS);
        log.info("Token blacklisted: tokenId={}, expirySeconds={}", tokenId, expirySeconds);
    }
}

