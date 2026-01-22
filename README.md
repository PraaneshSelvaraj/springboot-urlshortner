# URL Shortener

A microservice-based URL shortener built with Spring Boot, gRPC, and Docker


## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Local Development Setup](#local-development-setup)
- [Service Details](#service-details)
- [API Documentation](#api-documentation)
  - [URL Management APIs](#url-management-apis)
  - [Notification APIs](#notification-apis)
  - [User Management APIs](#user-management-apis)
  - [Authentication APIs](#authentication-apis)
  - [Health Check](#health-check)
  - [Web UI](#web-ui)
- [Configuration](#configuration)
  - [Environment Variables](#environment-variables)
  - [Port Configuration](#port-configuration)
- [Testing](#testing)
  - [Running Tests](#running-tests)

## Overview

URL Shortener is a microservices application that transforms long URLs into short links. Built with Spring Boot and gRPC, it provides a scalable, distributed architecture with separate services for URL management, notifications, and user authentication.

The microservices architecture enables independent scaling, deployment, and maintenance of each service. This design ensures high availability, better fault isolation, and allows different teams to work on different services simultaneously.

Key capabilities include URL shortening with validation, click tracking with threshold notifications, user authentication (local and Google OAuth2), JWT-based security, and a comprehensive notification system for monitoring URL events.

## Architecture

### Service Communication

- **Client → REST-SERVICE**: HTTP/HTTPS REST API for URL shortening, web UI access
- **REST-SERVICE ↔ NOTIFICATION-SERVICE**: gRPC for real-time event notifications
- **REST-SERVICE ↔ USER-SERVICE**: gRPC for authentication and user operations
- **All Services → MySQL**: JDBC connections for data persistence with separate schemas


## Features

### Core URL Shortening (REST-SERVICE)

- **7-Character Short Codes**: Generates unique, URL-safe short codes for every long URL
- **Click Tracking**: Counts every redirect and triggers notifications at configurable thresholds
- **URL Expiration**: Automatically expires URLs after a configurable period
- **Advanced Validation**:
  - DNS resolution checks to ensure URLs are reachable
  - Banned host filtering
  - Self-reference prevention to avoid loops
  - HTTP/HTTPS scheme validation
- **Pagination & Sorting**: List URLs with flexible sorting by ID, clicks, creation date, or expiration
- **Web UI**: User-friendly JSP interface for creating and managing short URLs
- **REST API**: Complete HTTP API for programmatic access

### Notification System (NOTIFICATION-SERVICE)

- **gRPC-Based Architecture**: High-performance notifications via Protocol Buffers
- **Three Notification Types**:
  - `NEWURL`: Triggered when a new URL is created
  - `THRESHOLD`: Triggered when click count exceeds threshold
  - `NEWUSER`: Triggered when a new user registers
- **Three Status Levels**: SUCCESS, PENDING, FAILURE for tracking notification delivery
- **Persistent Storage**: All notifications stored in MySQL
- **Paginated Retrieval**: Fetch notification history with sorting and pagination

### User Management & Authentication (USER-SERVICE)

- **Local Authentication**: Username/password with BCrypt hashing
- **Google OAuth2 Integration**: Seamless sign-in with Google accounts, automatic account creation
- **JWT-Based Security** (RS256 asymmetric signing):
  - Access tokens for API authentication
  - Refresh tokens for session renewal
- **Token Refresh Mechanism**: Server-side token storage for secure refresh flow
- **Full User CRUD**: Create, read, update, delete operations via gRPC (7 RPC methods)
- **User Ownership & Role-Based Access**: Users own their URLs and can only view/manage their own; ADMIN role can access all resources
- **Soft Deletes**: Users are flagged as deleted, not removed from database
- **Strong Password Requirements**: Enforces 8+ chars with uppercase, lowercase, digit, and special character

### Technical Features

- **Multi-Stage Docker Builds**: Optimized container images with separate build and runtime stages
- **gRPC JWT Propagation**: Authentication context automatically propagated across microservices
- **Comprehensive Test Coverage**: JUnit 5 tests with H2 in-memory database
- **Code Formatting Enforcement**: Google Java Format via CI pipeline
- **CI/CD Integration**: Automated testing and formatting checks on every commit

## Tech Stack

### Backend

- **Java 21**
- **Spring Boot**
- **Spring Data JPA**
- **Spring MVC**
- **Hibernate**

### Communication

- **gRPC**
- **Protocol Buffers**
- **REST API**
- **JSP**

### Security

- **JWT (JJWT)** with RS256 asymmetric signing
- **BCrypt**
- **Google OAuth2**

### Database

- **MySQL**
- **H2** (for testing)

### Tools

- **Docker & Docker Compose**
- **Maven**
- **Lombok**
- **JaCoCo**
- **Google Java Format**

## Prerequisites

- **Docker** and **Docker Compose**
- **Java 21** (for local development)
- **Maven** (for local development)
- **MySQL** (for local development)

## Getting Started

### Quick Start with Docker

The fastest way to run the entire application:

**1. Clone the repository**

```bash
git clone https://github.com/PraaneshSelvaraj/springboot-urlshortner.git
cd springboot-urlshortner
```

**2. Set up environment variables**

```bash
cp .env.example .env
```

**3. Edit .env file with your configuration**

Required: Update MySQL password and JWT secret
Optional: Add Google Client ID for OAuth2

**4. Start all services**

```bash
docker-compose up --build
```

**5. Access the application**

- REST Service: http://localhost:8080
- Web UI: http://localhost:8080/
- Notification Service: http://localhost:8081
- User Service: http://localhost:8082

### Local Development Setup

For development without Docker:

#### 1. Set Up MySQL

Start MySQL and create database:

```bash
mysql -u root -p
```

```sql
CREATE DATABASE urlshortner;
EXIT;
```

#### 2. Configure Environment Variables

Copy example files:

```bash
cp services/rest-service/.env.example services/rest-service/.env
```

```bash
cp services/user-service/.env.example services/user-service/.env
```

#### 3. Build and Install gRPC Common

```bash
cd services/grpc-common
mvn clean install
```

#### 4. Start Services

Open three terminal windows and run each service:

**Terminal 1: Notification Service**

```bash
cd services/notification-service
mvn spring-boot:run
```

**Terminal 2: User Service**

```bash
cd services/user-service
mvn spring-boot:run
```

**Terminal 3: REST Service**

```bash
cd services/rest-service
mvn spring-boot:run
```

#### 5. Access the Application

- REST API: http://localhost:8080/api
- Web UI: http://localhost:8080/
- API Documentation: See [API Documentation](#api-documentation) section


## Service Details

### REST-SERVICE (Port 8080)

**Purpose**: Primary entry point for URL shortening operations and web UI

**Responsibilities**:
- Accepts HTTP requests for URL shortening
- Validates and generates unique short codes
- Tracks clicks and manages URL expiration
- Serves JSP-based web interface
- Communicates with notification-service for events

**Key Configuration**:
```properties
bannedHosts=example.com
urlExpirationHours=720
notification.threshold=100
app.baseurl=http://localhost:8080
grpc.notification.host=localhost
grpc.notification.port=9091
```

### NOTIFICATION-SERVICE (HTTP: 8081, gRPC: 9091)

**Purpose**: Event notification management and logging

**Responsibilities**:
- Receives notifications via gRPC
- Stores notification events in MySQL
- Provides notification retrieval with pagination
- Tracks notification types and statuses

**gRPC Methods**:
- `notify(NotificationRequest) → NotificationReply`: Create a notification
- `getNotifications(GetNotificationsRequest) → GetNotificationsResponse`: Retrieve paginated notifications

**Notification Types**:
- `NEWURL`: New URL created
- `THRESHOLD`: Click threshold reached
- `NEWUSER`: New user registered

**Notification Statuses**:
- `SUCCESS`: Notification processed successfully
- `PENDING`: Awaiting processing
- `FAILURE`: Processing failed

### USER-SERVICE (HTTP: 8082, gRPC: 9092)

**Purpose**: User authentication and management

**Responsibilities**:

- User registration and profile management
- Local authentication (email/password)
- Google OAuth2 authentication
- JWT token generation and validation
- Token refresh mechanism

**gRPC Methods**:

- `createUser(CreateUserRequest) → User`: Register new user
- `userLogin(LoginRequest) → LoginResponse`: Authenticate with email/password
- `googleLogin(GoogleLoginRequest) → LoginResponse`: Authenticate with Google
- `getUserById(GetUserRequest) → User`: Retrieve user details
- `getUsers(GetUsersRequest) → GetUsersResponse`: Retrieve paginated list of users (ADMIN only)
- `deleteUserById(DeleteUserRequest) → DeleteUserResponse`: Soft delete user
- `refreshTokens(RefreshTokenRequest) → RefreshTokenResponse`: Generate new tokens
- `logoutUser(LogoutUserRequest) → LogoutUserResponse`: End user session

**JWT Configuration**:

```properties
jwt.access-token.expiration=3600000           # 1 hour in milliseconds
jwt.refresh-token.expiration=604800000        # 7 days in milliseconds
jwt.rsa.private-key=<path-to-private-key>     # RSA private key for signing
jwt.rsa.public-key=<path-to-public-key>       # RSA public key for verification
```

**Password Requirements**:

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (@$!%*?&)

**JWT Architecture & Security**:

The application uses **RS256 (RSA Signature with SHA-256)** asymmetric encryption for JWT tokens:

- **Token Types**:
  - **Access Token**: `type: "auth"`, expires in 1 hour, used for API authentication
  - **Refresh Token**: `type: "refresh"`, expires in 7 days, used for obtaining new access tokens

- **Token Claims**:
  - `sub`: User email
  - `userId`: User ID
  - `role`: User role (USER or ADMIN)
  - `type`: Token type ("auth" or "refresh")
  - `id`: JWT ID (JTI) - used for refresh token invalidation on logout

- **Key Distribution**:
  - **Private Key**: Used only by user-service for token signing
  - **Public Key**: Used by rest-service and notification-service for token verification

- **Token Storage**:
  - Access tokens are stateless (no server-side storage)
  - Refresh token JTI is stored in the database for blacklisting
  - On logout, the JTI is cleared, preventing future refresh attempts

**gRPC JWT Propagation (Commit 93a15f5)**:

JWT authentication context is automatically propagated across microservices:

- **Client-Side**: `GrpcAuthClientInterceptor` in rest-service extracts JWT from Spring SecurityContext and adds it to gRPC metadata headers
- **Server-Side**: `GrpcAuthServerInterceptor` in user-service and notification-service validates JWT and creates user context
- **Public Endpoints** (no authentication required):
  - User Service: `createUser`, `userLogin`, `googleLogin`, `refreshTokens`
  - Notification Service: `notify` (authentication removed in commit e911105)
- **Protected Endpoints**: All other gRPC methods require valid JWT with `type: "auth"`

### GRPC-COMMON

**Purpose**: Shared Protocol Buffer definitions

**Contents**:

- `notification.proto`: Notification service contract
- `user.proto`: User service contract

## API Documentation

**Authentication & Authorization:**

All API endpoints have specific authentication requirements:

- **URL Management APIs**:
  - `POST /api/urls`: Requires USER or ADMIN role
  - `GET /api/urls`: Requires USER or ADMIN role (users see only their own URLs, ADMIN sees all)
  - `GET /api/urls/{shortCode}`: Requires USER or ADMIN role (users can only view their own URLs)
  - `DELETE /api/urls/{shortCode}`: Requires USER or ADMIN role (users can only delete their own)
  - `GET /{shortCode}`: No authentication required (public redirect)

- **Notification APIs**:
  - `GET /api/notifications`: Requires ADMIN role only

- **User Management APIs**:
  - `POST /api/users`: No authentication required (public registration)
  - `GET /api/users`: Requires ADMIN role only
  - `GET /api/users/{id}`: Requires USER or ADMIN role (users can only view their own profile, ADMIN can view any)
  - `DELETE /api/users/{id}`: Requires USER or ADMIN role (users can only delete their own account, ADMIN can delete any)

- **Authentication APIs**:
  - `POST /api/auth/login`: No authentication required
  - `POST /api/auth/google/login`: No authentication required
  - `POST /api/auth/refresh`: Requires valid refresh token
  - `POST /api/auth/logout`: Requires valid access token

**User Ownership (Commit 78c4139):**
- When a URL is created, it's automatically mapped to the authenticated user via the `createdBy` field
- Users can only view, manage, and delete their own URLs
- ADMIN role users can access and manage all URLs regardless of ownership
- Attempting to access another user's resources results in `403 Forbidden` or filtered results

### URL Management APIs

#### 1. Create Short URL

Creates a shortened URL for the given long url

- **URL**: `/api/urls`
- **Method**: `POST`
- **Request Body**:

  ```json
  {
    "url": "https://example.com/"
  }
  ```

- **Response** (201 Created):

  ```json
  {
    "id": 1,
    "longUrl": "https://example.com/",
    "shortCode": "5eQsiTg",
    "shortUrl": "http://localhost:8080/5eQsiTg",
    "clicks": 0,
    "expired": false,
    "createdAt": "2025-01-06T12:30:07",
    "updatedAt": "2025-01-06T12:30:07",
    "expiresAt": "2025-02-05T12:30:07"
  }
  ```
- **Error Responses**:

  - `400 Bad Request`: Invalid URL format or banned host
  - `500 Internal Server Error`: Database or system error

#### 2. Redirect to Original URL

Redirects the user to the original long URL and increments click counter.

- **URL**: `/{shortCode}`
- **Method**: `GET`
- **Example**: `GET http://localhost:8080/5eQsiTg`
- **Response**: `307 Temporary Redirect` to original URL
- **Error Responses**:
  - `404 Not Found`: Short code does not exist
  - `410 Gone`: URL has expired
  - `429 Too Many Requests`: Click threshold exceeded

#### 3. List All URLs

Retrieves a paginated, sortable list of all shortened URLs.

- **URL**: `/api/urls`
- **Method**: `GET`
- **Query Parameters**:
  - `pageNo` (optional, default: `0`): Zero-based page number
  - `pageSize` (optional, default: `10`): Items per page
  - `sortBy` (optional): Sort field - `id`, `shortCode`, `clicks`, `createdAt`, `expiresAt`
  - `sortDirection` (optional): `asc` or `desc`
- **Example**: `GET /api/urls?pageNo=0&pageSize=10&sortBy=clicks&sortDirection=desc`
- **Response** (200 OK):

  ```json
  {
    "content": [
      {
        "id": 1,
        "longUrl": "https://example.com",
        "shortCode": "5eQsiTg",
        "shortUrl": "http://localhost:8080/5eQsiTg",
        "clicks": 42,
        "expired": false,
        "createdAt": "2025-01-06T12:30:07",
        "updatedAt": "2025-01-06T14:15:30",
        "expiresAt": "2025-02-05T12:30:07"
      }
    ],
    "pageable": {
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "pageNumber": 0,
      "pageSize": 10,
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 1,
    "size": 10,
    "number": 0,
    "first": true,
    "last": true,
    "empty": false
  }
  ```

#### 4. Get URL by Short Code

Retrieves detailed information about a specific shortened URL.

- **URL**: `/api/urls/{shortCode}`
- **Method**: `GET`
- **Example**: `GET /api/urls/5eQsiTg`
- **Response** (200 OK):

  ```json
  {
    "id": 1,
    "longUrl": "https://example.com",
    "shortCode": "5eQsiTg",
    "shortUrl": "http://localhost:8080/5eQsiTg",
    "clicks": 42,
    "expired": false,
    "createdAt": "2025-01-06T12:30:07",
    "updatedAt": "2025-01-06T14:15:30",
    "expiresAt": "2025-02-05T12:30:07"
  }
  ```

- **Error Response** (404 Not Found):

  ```json
  {
    "status": 404,
    "message": "Unable to find Url with shortcode 5eQsiTg"
  }
  ```

#### 5. Delete URL

Permanently removes a shortened URL from the system.

- **URL**: `/api/urls/{shortCode}`
- **Method**: `DELETE`
- **Example**: `DELETE /api/urls/5eQsiTg`
- **Response**: `204 No Content`
- **Error Response** (404 Not Found):

  ```json
  {
    "status": 404,
    "message": "Unable to find Url with shortcode 5eQsiTg"
  }
  ```

### Notification APIs

#### 6. Get All Notifications

Retrieves a paginated list of notification events. Requires ADMIN role.

- **URL**: `/api/notifications`
- **Method**: `GET`
- **Query Parameters**:
  - `pageNo` (optional, default: `0`): Page number
  - `pageSize` (optional, default: `10`): Items per page
  - `sortBy` (optional): Sort field - `id`, `shortCode`, `createdAt`
  - `sortDirection` (optional): `asc` or `desc`
- **Example**: `GET /api/notifications?pageNo=0&pageSize=10&sortBy=createdAt&sortDirection=desc`
- **Response** (200 OK):

  ```json
  {
    "notifications": [
      {
        "id": 1,
        "message": "New URL Created: https://example.com/",
        "shortCode": "5eQsiTg",
        "notificationType": "NEWURL",
        "notificationStatus": "SUCCESS"
      },
      {
        "id": 2,
        "message": "Threshold reached for shortcode - '5eQsiTg'",
        "shortCode": "5eQsiTg",
        "notificationType": "THRESHOLD",
        "notificationStatus": "SUCCESS"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalPages": 1,
    "totalElements": 2
  }
  ```

### User Management APIs

#### 7. Create User

Registers a new user with local authentication.

- **URL**: `/api/users`
- **Method**: `POST`
- **Authentication**: None (public registration endpoint)
- **Request Body**:

  ```json
  {
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }
  ```

- **Response** (201 Created):

  ```json
  {
    "id": 1,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "USER",
    "authProvider": "LOCAL",
    "createdAt": "2025-01-06T12:30:07",
    "updatedAt": "2025-01-06T12:30:07"
  }
  ```

- **Validation Rules**:
  - Username: 3-30 characters, alphanumeric with hyphens and underscores
  - Email: Valid email format
  - Password: Minimum 8 chars with uppercase, lowercase, digit, and special character

#### 8. Get User by ID

Retrieves user details by id. Requires authentication (users can only view their own profile, ADMIN can view any).

- **URL**: `/api/users/{id}`
- **Method**: `GET`
- **Example**: `GET /api/users/1`
- **Response** (200 OK):

  ```json
  {
    "id": 1,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "USER",
    "authProvider": "LOCAL",
    "createdAt": "2025-01-06T12:30:07",
    "updatedAt": "2025-01-06T12:30:07"
  }
  ```

- **Error Response** (404 Not Found):

  ```json
  {
    "status": 404,
    "message": "User not found or has been deleted"
  }
  ```

#### 9. Delete User

Performs a soft delete on a user account (sets `isDeleted` flag). Requires authentication (users can only delete their own account, ADMIN can delete any).

- **URL**: `/api/users/{id}`
- **Method**: `DELETE`
- **Example**: `DELETE /api/users/1`
- **Response**: `204 No Content`

#### 10. Get All Users

Retrieves a paginated list of all users. Requires ADMIN role.

- **URL**: `/api/users`
- **Method**: `GET`
- **Authentication**: Required (ADMIN role only)
- **Authorization**: `@PreAuthorize("hasRole('ADMIN')")`
- **Query Parameters**:
  - `pageNo` (optional, default: `0`): Page number
  - `pageSize` (optional, default: `10`): Items per page
  - `sortBy` (optional): Sort field - `id`, `username`, `email`, `role`, `authProvider`, `createdAt`, `updatedAt`
  - `sortDirection` (optional): `asc` or `desc`
- **Example**: `GET /api/users?pageNo=0&pageSize=10&sortBy=createdAt&sortDirection=desc`
- **Response** (200 OK):

  ```json
  {
    "users": [
      {
        "id": 1,
        "username": "johndoe",
        "email": "john.doe@example.com",
        "role": "USER",
        "authProvider": "LOCAL",
        "createdAt": "2025-01-06T12:30:07",
        "updatedAt": "2025-01-06T12:30:07"
      },
      {
        "id": 2,
        "username": "admin",
        "email": "admin@example.com",
        "role": "ADMIN",
        "authProvider": "LOCAL",
        "createdAt": "2025-01-05T10:20:15",
        "updatedAt": "2025-01-05T10:20:15"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalPages": 1,
    "totalElements": 2
  }
  ```

### Authentication APIs

#### 11. Local Login

Authenticates a user with email and password, returns JWT tokens.

- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Request Body**:

  ```json
  {
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
  }
  ```

- **Response** (200 OK):

  ```json
  {
    "message": "Login was Successful",
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3MDQ2Mzg0MDcsImlhdCI6MTcwNDYzNDgwNywiZW1haWwiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsInJvbGUiOiJVU0VSIn0...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3MDUyMzk2MDcsImlhdCI6MTcwNDYzNDgwNywiZW1haWwiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsInJvbGUiOiJVU0VSIiwidHlwZSI6InJlZnJlc2gifQ..."
  }
  ```

- **Error Responses**:
  - `401 Unauthorized`: Invalid email or password
  - `403 Forbidden`: Account is deleted or uses different auth provider

#### 12. Google OAuth2 Login

Authenticates or registers a user with Google OAuth2 ID token.

- **URL**: `/api/auth/google/login`
- **Method**: `POST`
- **Request Body**:

  ```json
  {
    "idToken": "google-id-token-from-frontend-oauth-flow"
  }
  ```

- **Response** (200 OK):

  ```json
  {
    "message": "Account created and login successful",
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
  }
  ```

- **Note**: If user doesn't exist, account is automatically created
- **Error Response** (401 Unauthorized):

  ```json
  {
    "status": 401,
    "message": "Invalid Google ID token"
  }
  ```

#### 13. Refresh Tokens

Generates new access and refresh tokens using a valid refresh token.

- **URL**: `/api/auth/refresh`
- **Method**: `POST`
- **Headers**:

  ```
  Authorization: Bearer <refresh_token>
  ```

- **Response** (200 OK):

  ```json
  {
    "message": "Tokens refreshed successfully.",
    "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
  }
  ```

- **Error Responses**:
  - `401 Unauthorized`: Invalid or expired refresh token
  - `403 Forbidden`: Token doesn't match stored token

#### 14. Logout

Logs out the current user and invalidates refresh tokens.

- **URL**: `/api/auth/logout`
- **Method**: `POST`
- **Authentication**: Required (Bearer access token)
- **Headers**:

  ```
  Authorization: Bearer <access_token>
  ```

- **Response** (200 OK):

  ```json
  {
    "success": true,
    "message": "Logged out successfully"
  }
  ```

- **Description**: Clears the refresh token JTI from the database, preventing future token refresh attempts. The access token becomes invalid for refresh operations.

### Health Check

#### 15. Health Check

Checks if the REST service is running and operational.

- **URL**: `/health`
- **Method**: `GET`
- **Response** (200 OK):

  ```json
  {
    "status": "OK",
    "message": "Rest Service is running.",
    "timestamp": "2025-01-06T12:30:07.123Z"
  }
  ```

### Web UI

#### Home Page

- **URL**: `/`
- **Method**: `GET`
- **Description**: Web form for creating shortened URLs
- **Features**: URL input, submit button, displays shortened URL result

#### URLs Management Page

- **URL**: `/urls`
- **Method**: `GET`
- **Query Parameters**:
  - `page` (optional, default: `0`): Page number
  - `size` (optional, default: `10`): Items per page
  - `sortBy` (optional): Sort field
  - `sortDirection` (optional): `asc` or `desc`
- **Description**: Table view of all shortened URLs with pagination controls

## Configuration

### Environment Variables

All services are configured via environment variables defined in `.env` file:

#### Database Configuration

```bash
# MySQL root password for Docker container
MYSQL_ROOT_PASSWORD=your_secure_password_here

# Database name (shared across services)
MYSQL_DATABASE=urlshortner

# Database credentials
DB_USERNAME=root
DB_PASSWORD=your_secure_password_here

# Database host (use 'mysql' for Docker, 'localhost' for local dev)
DB_HOST=mysql
DB_PORT=3306
```

#### Application Configuration

```bash
# Base URL for shortened links (used in response shortUrl field)
APP_BASE_URL=http://localhost:8080

# Comma-separated list of banned hostnames
BANNED_HOSTS=example.com,spam-site.com

# Hours until URL expires (720 = 30 days)
URL_EXPIRATION_HOURS=720

# Click count threshold for notifications
NOTIFICATION_THRESHOLD=100
```

#### JWT Configuration

```bash
# JWT uses RS256 (RSA Signature with SHA-256) asymmetric encryption
# Generate RSA key pair:
openssl genrsa -out private_key.pem 2048
openssl rsa -in private_key.pem -pubout -out public_key.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private_key.pem -out private_key_pkcs8.pem

# Base64 encode keys (single line, no headers/footers):
cat private_key_pkcs8.pem | grep -v "BEGIN" | grep -v "END" | tr -d '\n'
cat public_key.pem | grep -v "BEGIN" | grep -v "END" | tr -d '\n'

# Base64-encoded PKCS8 private key for JWT signing (user-service only)
JWT_RSA_PRIVATE_KEY=your-base64-encoded-private-key-here

# Base64-encoded public key for JWT verification (all services)
JWT_RSA_PUBLIC_KEY=your-base64-encoded-public-key-here

# Access token expiration in milliseconds (3600000 = 1 hour)
JWT_ACCESS_TOKEN_EXPIRATION=3600000

# Refresh token expiration in milliseconds (604800000 = 7 days)
JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

#### Google OAuth2 Configuration

```bash
# OAuth 2.0 Client ID from Google Cloud Console
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

### Port Configuration

| Service | HTTP Port | gRPC Port |
|---------|-----------|-----------|
| **REST-SERVICE** | 8080 | - |
| **NOTIFICATION-SERVICE** | 8081 | 9091 |
| **USER-SERVICE** | 8082 | 9092 |

## Testing

### Running Tests

**1. Build and install gRPC common module**

```bash
cd services/grpc-common
mvn clean install
```

**2. Run tests for rest-service**

```bash
cd ../rest-service
mvn test
```

**3. Run tests for notification-service**

```bash
cd ../notification-service
mvn test
```

**4. Run tests for user-service**

```bash
cd ../user-service
mvn test
```

**5. Generate code coverage report**

```bash
mvn test jacoco:report
```

View report at: `target/site/jacoco/index.html`
