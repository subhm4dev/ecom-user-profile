package com.ecom.profile.service;

import com.ecom.httpclient.client.ResilientWebClient;
import com.ecom.profile.config.JwtConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWKS Service
 * 
 * <p>Fetches and caches JSON Web Key Set (JWKS) from Identity service.
 * Periodically refreshes keys to support key rotation.
 * 
 * <p>Uses ResilientWebClient with circuit breaker, retry, and rate limiting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwksService {

    private final ResilientWebClient resilientWebClient;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private WebClient webClient; // Lazy initialization

    private final Map<String, RSAKey> jwkCache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;

    /**
     * Get WebClient instance (lazy initialization)
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = resilientWebClient.create("identity-service", jwtConfig.getIdentityServiceUrl());
        }
        return webClient;
    }

    /**
     * Get public key by Key ID (kid)
     * 
     * @param kid Key ID from JWT header
     * @return RSAKey with the public key
     * @throws IllegalArgumentException if key not found
     */
    public RSAKey getPublicKey(String kid) {
        RSAKey key = jwkCache.get(kid);
        if (key != null) {
            return key;
        }

        // Cache miss - refresh and try again
        log.warn("JWK key not found in cache: kid={}, refreshing cache...", kid);
        refreshJwksCache();
        
        RSAKey refreshedKey = jwkCache.get(kid);
        if (refreshedKey == null) {
            throw new IllegalArgumentException("JWK key not found after refresh: " + kid);
        }
        return refreshedKey;
    }

    /**
     * Refresh JWKS cache from Identity service
     * 
     * Note: @Scheduled uses fixedDelay in milliseconds.
     */
    @Scheduled(fixedDelayString = "${jwt.jwks-cache-refresh-interval-ms:300000}")
    public void refreshJwksCache() {
        log.debug("Refreshing JWKS cache from Identity service...");

        try {
            String jwksEndpoint = jwtConfig.getJwksEndpoint();
            
            // Use ResilientWebClient with circuit breaker, retry, and rate limiting
            String responseBody = getWebClient()
                .get()
                .uri(jwksEndpoint)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Failed to fetch JWKS: status={}, message={}", ex.getStatusCode(), ex.getMessage());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Failed to fetch JWKS from Identity service", ex);
                    return Mono.empty();
                })
                .block(); // Blocking call (we're in a scheduled method)

            if (responseBody == null || responseBody.isBlank()) {
                log.error("Empty JWKS response from Identity service");
                return;
            }

            // Extract JWKS from wrapped response (if wrapped in ApiResponse)
            String jwksJson = extractJwksFromResponse(responseBody);

            // Parse JWKS
            JWKSet jwkSet = JWKSet.parse(jwksJson);
            Map<String, RSAKey> newCache = new ConcurrentHashMap<>();

            for (JWK jwk : jwkSet.getKeys()) {
                if (jwk instanceof RSAKey) {
                    RSAKey rsaKey = (RSAKey) jwk;
                    newCache.put(rsaKey.getKeyID(), rsaKey);
                    log.debug("Cached JWK: kid={}", rsaKey.getKeyID());
                }
            }

            jwkCache.clear();
            jwkCache.putAll(newCache);
            lastFetchTime = System.currentTimeMillis();

            log.info("JWKS cache refreshed: {} keys cached", jwkCache.size());

        } catch (ParseException e) {
            log.error("Failed to parse JWKS response from Identity service", e);
        } catch (Exception e) {
            log.error("Failed to fetch JWKS from Identity service", e);
        }
    }

    /**
     * Extract JWKS JSON from wrapped ApiResponse
     * 
     * <p>Identity service wraps JWKS in standard ApiResponse format:
     * {"success": true, "data": {"keys": [...]}}
     */
    private String extractJwksFromResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            
            // Check if wrapped in ApiResponse format
            if (rootNode.has("data") && rootNode.get("data").has("keys")) {
                // Return just the data part (JWKS object)
                return rootNode.get("data").toString();
            }
            
            // If not wrapped, return as-is
            return responseBody;
        } catch (Exception e) {
            log.warn("Failed to extract JWKS from response, using raw response", e);
            return responseBody;
        }
    }

    /**
     * Get cached keys count (for monitoring)
     */
    public int getCachedKeysCount() {
        return jwkCache.size();
    }
}

