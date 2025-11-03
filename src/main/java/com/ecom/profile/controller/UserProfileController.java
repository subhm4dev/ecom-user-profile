package com.ecom.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Profile Controller
 * 
 * <p>This controller manages user profile information including personal details
 * like full name, phone number, and avatar. Profiles enhance user experience
 * by personalizing interactions across the platform.
 * 
 * <p>Why we need these APIs:
 * <ul>
 *   <li><b>Profile Creation/Update:</b> Allows users to complete their profile after
 *       registration, storing additional information not captured during sign-up.
 *       Essential for personalized UX and order fulfillment.</li>
 *   <li><b>Profile Retrieval:</b> Enables other services (e.g., checkout, orders)
 *       to fetch user information for displaying names, avatars, and contact details
 *       without duplicating data.</li>
 *   <li><b>Event-Driven Updates:</b> Profile changes are published to Kafka,
 *       enabling other services to react to profile updates (e.g., update order
 *       display, notification preferences).</li>
 * </ul>
 * 
 * <p>This service integrates with the Identity service (via user_id) but maintains
 * separate profile data, following microservice separation of concerns.
 */
@RestController
@RequestMapping("/v1/profile")
@Tag(name = "User Profile", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    /**
     * Create or update user profile
     * 
     * <p>This endpoint creates a new profile or updates an existing one for the
     * authenticated user. The user ID is extracted from the JWT token via Gateway
     * headers (X-User-Id).
     * 
     * <p>Profile updates trigger a Kafka event (ProfileUpdated) that other services
     * can consume to sync profile changes.
     * 
     * <p>This endpoint is protected and requires authentication. Users can only
     * update their own profile.
     */
    @PostMapping
    @Operation(
        summary = "Create or update user profile",
        description = "Creates a new user profile or updates existing profile. Publishes ProfileUpdated event to Kafka."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> createOrUpdateProfile(@Valid @RequestBody Object profileRequest) {
        // TODO: Implement profile create/update logic
        // 1. Extract userId from X-User-Id header (via tenant-context-starter)
        // 2. Validate profileRequest DTO (fullName, phone, avatarUrl)
        // 3. Check if profile exists for userId
        // 4. Create new UserProfile entity or update existing one
        // 5. Persist to database
        // 6. Publish ProfileUpdated event to Kafka
        // 7. Return profile response with status
        return ResponseEntity.ok(null);
    }

    /**
     * Get user profile by user ID
     * 
     * <p>This endpoint retrieves profile information for a specified user. It's used
     * by other services and the frontend to display user information.
     * 
     * <p>Access control:
     * <ul>
     *   <li>Users can view their own profile</li>
     *   <li>Admins/Sellers can view customer profiles (for order management)</li>
     * </ul>
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user profile by ID",
        description = "Retrieves profile information for the specified user ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getProfile(@PathVariable UUID userId) {
        // TODO: Implement profile retrieval logic
        // 1. Extract currentUserId from X-User-Id header
        // 2. Check authorization: user can view own profile, admins can view any
        // 3. Find UserProfile entity by userId
        // 4. Return profile response or 404 if not found
        // 5. Handle authorization errors (403 Forbidden)
        return ResponseEntity.ok(null);
    }

    /**
     * Get current user's profile
     * 
     * <p>Convenience endpoint that returns the profile of the authenticated user,
     * eliminating the need to extract userId from token on the client side.
     * 
     * <p>This endpoint is protected and requires authentication.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user's profile",
        description = "Retrieves the profile of the currently authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getMyProfile() {
        // TODO: Implement current user profile retrieval
        // 1. Extract userId from X-User-Id header
        // 2. Find UserProfile entity by userId
        // 3. Return profile response or 404 if not found
        return ResponseEntity.ok(null);
    }
}

