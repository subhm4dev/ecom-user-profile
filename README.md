# 🔧 E-Commerce User Profile Service

User Profile Management Service for the E-Commerce Platform.

## Overview

This service manages user profile information including name, phone number, and avatar URL.

## Port

**8082**

## Features

- Create/Update user profile
- Get user profile by userId
- Publish ProfileUpdated events to Kafka

## Running Locally

```bash
mvn spring-boot:run
```

## Endpoints

- `POST /v1/profile` - Create or update profile
- `GET /v1/profile/{userId}` - Get profile by userId

## Dependencies

- PostgreSQL (via Docker Compose)
- Kafka (for event publishing)
- Spring Cloud Config Server (optional)

