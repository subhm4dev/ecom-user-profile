package com.ecom.profile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT Configuration Properties
 * 
 * <p>Configuration for JWT validation and JWKS endpoint.
 * Loaded from application.yml under 'jwt' prefix.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    
    /**
     * Identity service URL for fetching JWKS
     */
    private String identityServiceUrl = "http://localhost:8081";
    
    /**
     * JWKS endpoint path
     */
    private String jwksEndpoint = "/.well-known/jwks.json";
    
    /**
     * JWKS cache refresh interval in milliseconds
     */
    private long jwksCacheRefreshIntervalMs = 300000; // 5 minutes
}

