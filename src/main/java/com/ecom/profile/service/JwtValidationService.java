package com.ecom.profile.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT Validation Service
 *
 * <p>Validates JWT tokens, extracts claims, and checks expiry.
 * Works with JwksService to get public keys for signature verification.
 * 
 * <p>This is a blocking (MVC) version, not reactive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtValidationService {

    private final JwksService jwksService;
    private final SessionService sessionService;

    /**
     * Validate JWT token and extract claims
     *
     * @param token JWT token string
     * @return JWTClaimsSet with validated claims
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public JWTClaimsSet validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        try {
            // Parse JWT token
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Extract Key ID from header
            String kid = signedJWT.getHeader().getKeyID();
            if (kid == null) {
                throw new IllegalArgumentException("JWT token missing Key ID (kid)");
            }

            // Extract token ID for blacklist check
            String tokenId = extractTokenId(token);
            if (sessionService.isTokenBlacklisted(tokenId)) {
                throw new IllegalArgumentException("JWT token has been revoked");
            }

            // Get public key from JWKS cache
            RSAKey publicKey = jwksService.getPublicKey(kid);

            // Verify signature
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            // Get claims
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Check expiry
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(Date.from(Instant.now()))) {
                throw new IllegalArgumentException("JWT token has expired");
            }

            // Check issuer (optional, for additional security)
            String issuer = claimsSet.getIssuer();
            if (issuer != null && !issuer.equals("ecom-identity")) {
                log.warn("JWT token from unexpected issuer: {}", issuer);
            }

            return claimsSet;

        } catch (ParseException e) {
            log.error("Failed to parse JWT token", e);
            throw new IllegalArgumentException("Invalid JWT token format", e);
        } catch (JOSEException e) {
            log.error("JOSE error during token validation", e);
            throw new IllegalArgumentException("JWT signature verification failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during token validation", e);
            throw new IllegalArgumentException("Token validation failed", e);
        }
    }

    /**
     * Extract token ID (jti) from token
     * Used for blacklist checking
     */
    public String extractTokenId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            if (jti == null || jti.isBlank()) {
                // Fallback: use token hash
                return String.valueOf(token.hashCode());
            }
            return jti;
        } catch (Exception e) {
            log.error("Failed to extract token ID", e);
            // Fallback: use token hash
            return String.valueOf(token.hashCode());
        }
    }

    /**
     * Extract user ID from token claims
     */
    public String extractUserId(JWTClaimsSet claims) {
        // Try userId claim first, fallback to subject
        String userId = claims.getClaim("userId") != null
            ? claims.getClaim("userId").toString()
            : claims.getSubject();

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("JWT token missing user ID");
        }

        return userId;
    }

    /**
     * Extract tenant ID from token claims
     */
    public String extractTenantId(JWTClaimsSet claims) {
        Object tenantIdObj = claims.getClaim("tenantId");
        if (tenantIdObj == null) {
            throw new IllegalArgumentException("JWT token missing tenant ID");
        }
        return tenantIdObj.toString();
    }

    /**
     * Extract roles from token claims
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(JWTClaimsSet claims) {
        Object rolesObj = claims.getClaim("roles");
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return List.of(); // Return empty list if no roles
    }
}

