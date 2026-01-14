# AGENTS.md - Development Guide for AI Agents

> 📘 **Purpose:** This file helps AI agents (and developers) understand the project architecture, development workflow, and common commands.

---

## 🚀 Quick Commands

### Development
```bash
# Start development environment (Docker Compose)
docker compose up --build

# Stop development environment
docker compose down

# Check if containers are running
docker compose ps
```

### Build & Test
```bash
# Build project (typecheck + compile)
./mvnw clean compile

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserServiceTest

# Run tests with coverage report
./mvnw clean test jacoco:report
# View coverage: target/site/jacoco/index.html

# Run integration tests
./mvnw verify

# Package application
./mvnw clean package

# Run application locally (without Docker)
./mvnw spring-boot:run
```

**Note:** On Windows, use `.\mvnw` instead of `./mvnw` (if Maven not in PATH)

**IntelliJ IDEA:**
- Run tests: Right-click on test file/folder → **Run Tests**
- With coverage: Right-click → **Run with Coverage**

### Database
```bash
# Run Flyway migrations
./mvnw flyway:migrate

# Check migration status
./mvnw flyway:info

# Rollback last migration (careful!)
./mvnw flyway:undo
```

---

## 📁 Project Structure

```
demo/
├── src/main/java/com/baohoanhao/demo/
│   ├── controller/          # REST API endpoints
│   │   ├── AuthController.java        # /api/auth/** - Authentication
│   │   ├── OAuthController.java       # /api/auth/oauth2/** - OAuth2
│   │   └── UserController.java        # /api/users/** - User management
│   │
│   ├── service/             # Business logic layer
│   │   ├── AuthService.java           # Login, register, token management
│   │   ├── Oauth2LoginService.java    # Google/Facebook OAuth2 logic
│   │   └── StateService.java          # OAuth2 state validation
│   │
│   ├── security/            # Security components
│   │   ├── JwtService.java            # JWT creation & validation
│   │   ├── JwtAuthenticationFilter.java  # JWT filter for requests
│   │   ├── TokenStorageService.java   # Redis token storage & blacklist
│   │   └── CustomAuthenticationEntryPoint.java  # 401 error handler
│   │
│   ├── repository/          # Database access (Spring Data JPA)
│   │   └── UserRepository.java
│   │
│   ├── entity/              # Database entities
│   │   ├── User.java                  # users table
│   │   └── Role.java                  # USER, ADMIN enum
│   │
│   ├── dto/                 # Data Transfer Objects
│   │   ├── request/                   # API request DTOs
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   └── RefreshTokenRequest.java
│   │   └── response/                  # API response DTOs
│   │       ├── ApiResponse.java       # Standard API response wrapper
│   │       └── AuthResponse.java      # Login/register response
│   │
│   ├── exception/           # Custom exceptions
│   │   ├── BusinessException.java     # Base exception
│   │   ├── UnauthorizedException.java # 401 errors
│   │   ├── BadRequestException.java   # 400 errors
│   │   ├── NotFoundException.java     # 404 errors
│   │   └── GlobalExceptionHandler.java # Global exception handler
│   │
│   └── config/              # Spring Boot configuration
│       ├── SecurityConfig.java        # Spring Security config
│       ├── JwtProperties.java         # JWT settings from application.yaml
│       └── AdminBootstrap.java        # Create admin user on startup
│
├── src/main/resources/
│   ├── application.yaml               # Main configuration
│   ├── application-oauth.yaml         # OAuth2 configuration
│   └── db/migration/                  # Flyway migrations
│       └── V1__Create_Users_Table.sql
│
├── docs/                    # Documentation
│   ├── AGENTS.md                      # This file (development guide)
│   ├── USER_MANAGEMENT_API.md         # User management API design
│   ├── SWAGGER_GUIDE.md               # API documentation guide
│   └── K8S_CICD_GUIDE.md              # Kubernetes & CI/CD guide
│
├── k8s/                     # Kubernetes manifests
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
│
├── docker-compose.yml       # Development environment
├── Dockerfile               # Production image
└── pom.xml                  # Maven dependencies
```

---

## 🏗️ Architecture Overview

### 🔄 Request Flow

```
Client Request
    ↓
CORS Filter (SecurityConfig)
    ↓
JwtAuthenticationFilter (validate JWT, set Authentication)
    ↓
Spring Security FilterChain (check authorization)
    ↓
Controller (@RestController) - Handle request
    ↓
Service (@Service) - Business logic
    ↓
Repository (@Repository) - Database access
    ↓
MySQL Database
```

### 🔐 Authentication Flow

```
1. Register/Login
   POST /api/auth/register or /api/auth/login
   → AuthService validates credentials
   → JwtService generates access + refresh tokens
   → TokenStorageService saves refresh token to Redis
   → Return tokens to client

2. Authenticated Request
   GET /api/users/profile
   → JwtAuthenticationFilter extracts token from header
   → JwtService validates token (signature, expiration)
   → TokenStorageService checks if token is blacklisted
   → Set Authentication in SecurityContext
   → Controller receives authenticated user info

3. Token Refresh
   POST /api/auth/refresh
   → AuthService validates refresh token
   → Check token in Redis (not blacklisted)
   → Generate new access token
   → Return new token to client

4. Logout
   POST /api/auth/logout
   → Add access token to blacklist (Redis, TTL = remaining time)
   → Add refresh token to blacklist (Redis, TTL = remaining time)
   → Remove refresh token from active tokens
```

---

## 🛠️ Development Workflow

### Adding a New Feature

**Example: Add "Update User Profile" API**

#### 1. **Design API** (Document in `docs/`)
```markdown
PUT /api/users/profile
Request: { fullName, email, phone }
Response: { success, message, data }
```

#### 2. **Create DTO** (`dto/request/`, `dto/response/`)
```java
// UpdateProfileRequest.java
@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
```

#### 3. **Implement Service** (`service/`)
```java
// UserService.java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        // Business logic here
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));
        
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        
        userRepository.save(user);
        
        return UserProfileResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .build();
    }
}
```

#### 4. **Create Controller Endpoint** (`controller/`)
```java
// UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName(); // Get from JWT
        UserProfileResponse response = userService.updateProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", response));
    }
}
```

#### 5. **Update SecurityConfig** (if needed)
```java
// SecurityConfig.java
// /api/users/** already requires authentication by default
// Only add to PUBLIC_ENDPOINTS if endpoint should be public
```

#### 6. **Write Tests**
```java
// UserServiceTest.java
@Test
void updateProfile_Success() {
    // Arrange
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("New Name");
    
    // Act
    UserProfileResponse response = userService.updateProfile(userId, request);
    
    // Assert
    assertEquals("New Name", response.getFullName());
}
```

#### 7. **Test with Swagger**
- Start app: `docker compose up`
- Open: http://localhost:8080/swagger-ui/index.html
- Authorize with Bearer token
- Try endpoint

---

## 🔑 Key Concepts

### 1. **JWT Authentication**
- **Access Token**: 15 minutes, sent in `Authorization: Bearer <token>` header
- **Refresh Token**: 7 days, stored in Redis, used to get new access token
- **Blacklist**: Revoked tokens stored in Redis until expiration

### 2. **Spring Security**
- `SecurityConfig`: Define public/protected endpoints
- `JwtAuthenticationFilter`: Validate JWT on every request
- `Authentication authentication`: Access current user info in controllers

### 3. **Exception Handling**
- All exceptions handled by `GlobalExceptionHandler`
- Return consistent JSON format:
```json
{
  "success": false,
  "message": "Error message",
  "errors": { ... }
}
```

### 4. **API Response Format**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

### 5. **Database Migrations**
- Use Flyway for schema changes
- Files: `src/main/resources/db/migration/V{version}__{description}.sql`
- Naming: `V2__Add_Phone_To_Users.sql`
- Run: `./mvnw flyway:migrate` or auto-run on app start

### 6. **Redis Usage**
- **Refresh Tokens**: `refresh_token:{userId}` → token value
- **Blacklist**: `blacklist:{token}` → "revoked" (TTL = token remaining time)
- **OAuth2 State**: `oauth2_state:{state}` → user info (TTL = 5 minutes)

---

## 🧪 Testing Strategy

### Unit Tests
- Test business logic in `Service` classes
- Mock dependencies with `@Mock` and `@InjectMocks`
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
}
```

### Integration Tests
- Test full request/response flow
- Use `@SpringBootTest` and `MockMvc`
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void updateProfile_Returns200() throws Exception {
        mockMvc.perform(put("/api/users/profile")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"Test\"}"))
            .andExpect(status().isOk());
    }
}
```

---

## 📝 Code Style & Conventions

### Naming Conventions
- **Controllers**: `{Entity}Controller.java` (e.g., `UserController`)
- **Services**: `{Entity}Service.java` (e.g., `AuthService`)
- **DTOs**: `{Action}{Entity}Request/Response.java` (e.g., `UpdateProfileRequest`)
- **Exceptions**: `{Type}Exception.java` (e.g., `NotFoundException`)

### Request Mapping Patterns
- Auth endpoints: `/api/auth/**`
- Resource endpoints: `/api/{resources}/**` (e.g., `/api/users/profile`)

### Response Format
Always wrap responses in `ApiResponse<T>`:
```java
return ResponseEntity.ok(ApiResponse.success("Message", data));
```

### Validation
Use Jakarta Validation annotations:
```java
@NotBlank(message = "...")
@Email(message = "...")
@Size(min = 2, max = 100, message = "...")
@Pattern(regexp = "...", message = "...")
```

---

## 🔒 Security Checklist

When adding new endpoints:

- [ ] ✅ Add to `PUBLIC_ENDPOINTS` in `SecurityConfig` if public
- [ ] ✅ Use `Authentication authentication` parameter to get current user
- [ ] ✅ Validate user can only access their own resources (authorization)
- [ ] ✅ Never log or expose sensitive data (passwords, tokens)
- [ ] ✅ Use `@Valid` for request validation
- [ ] ✅ Return appropriate HTTP status codes (200, 400, 401, 403, 404)
- [ ] ✅ Add proper error messages in exception handlers

---

## 📚 Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 4.0.1 | Framework |
| Spring Security | 6.x | Authentication & authorization |
| JWT (jjwt) | 0.12.6 | Token generation & validation |
| MySQL | 8.0 | Database |
| Redis | 7.x | Token storage & caching |
| Flyway | 10.x | Database migrations |
| Lombok | Latest | Reduce boilerplate code |
| Maven | 3.11+ | Build tool |
| Docker | Latest | Containerization |

---

## 🐛 Common Issues & Solutions

### 1. **Token expired after 15 minutes**
✅ **Solution:** Frontend should auto-refresh token using `/api/auth/refresh`

### 2. **401 Unauthorized on protected endpoints**
❌ **Check:**
- Token sent in header: `Authorization: Bearer <token>`
- Token not expired
- Token not blacklisted (logout)
- Endpoint not in `PUBLIC_ENDPOINTS`

### 3. **Email/Phone already exists**
❌ **Check:** Database constraints (`@Column(unique = true)`)
✅ **Handle:** Throw `BadRequestException("Email đã được sử dụng")`

### 4. **Flyway migration failed**
❌ **Fix:**
```bash
# Repair flyway
./mvnw flyway:repair

# Or drop database and recreate
docker compose down -v
docker compose up
```

### 5. **Redis connection refused**
❌ **Check:** Redis container running (`docker compose ps`)
✅ **Fix:** `docker compose up redis`

---

## 📖 Additional Documentation

- **API Documentation:** http://localhost:8080/swagger-ui/index.html
- **API Spec (JSON):** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health
- **User Management API:** [docs/USER_MANAGEMENT_API.md](./USER_MANAGEMENT_API.md)
- **Kubernetes Deployment:** [docs/K8S_CICD_GUIDE.md](./K8S_CICD_GUIDE.md)

---

## 🎯 Next Steps for AI Agents

When implementing a new feature:

1. **Read this file first** to understand architecture
2. **Check existing code** for similar patterns (e.g., other controllers/services)
3. **Follow the workflow** in "Adding a New Feature" section
4. **Use consistent naming** and code style
5. **Write tests** before marking feature complete
6. **Test with Swagger** to verify API works
7. **Update documentation** if needed

---

## 📞 Getting Current User in Controller

```java
@GetMapping("/profile")
public ResponseEntity<ApiResponse<UserProfile>> getProfile(Authentication authentication) {
    // Get userId from JWT (set by JwtAuthenticationFilter)
    String userId = authentication.getName();
    
    // Get user role
    String role = authentication.getAuthorities().iterator().next().getAuthority();
    // Returns: "ROLE_USER" or "ROLE_ADMIN"
    
    // Use in service
    UserProfile profile = userService.getProfile(userId);
    
    return ResponseEntity.ok(ApiResponse.success("Success", profile));
}
```

---

## 🔧 Environment Variables

See `.env.example` or `application.yaml` for configuration:

```yaml
# JWT
JWT_SECRET: your-secret-key-here
jwt.access-token-expiration: 900000      # 15 minutes
jwt.refresh-token-expiration: 604800000  # 7 days

# Database
SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/demo_db
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: root

# Redis
SPRING_DATA_REDIS_HOST: localhost
SPRING_DATA_REDIS_PORT: 6379

# Admin Bootstrap
APP_ADMIN_EMAIL: admin@example.com
APP_ADMIN_PASSWORD: admin123
```

---

## 🧪 Test Coverage Results

### Unit Tests Summary

**Total: 57 unit tests** covering all 4 services

#### ✅ **UserServiceTest**: 16 tests
- `updateProfile()`: 8 tests (happy path + edge cases + errors)
  - Valid full name update
  - Unique email update
  - Duplicate email throws BadRequestException
  - Same email skips uniqueness check
  - Unique phone update
  - Duplicate phone throws BadRequestException
  - Same phone skips uniqueness check
  - User not found throws ResourceNotFoundException
- `deleteAccount()`: 6 tests (happy path + edge cases + errors)
  - Valid password deactivates account
  - Revokes all tokens on deletion
  - Incorrect password throws BadRequestException
  - Admin user throws ForbiddenException
  - User not found throws ResourceNotFoundException
  - Invalid UUID throws BadRequestException

#### ✅ **AuthServiceTest**: 19 tests
- `register()`: 5 tests
  - Register with email successfully
  - Register with phone successfully
  - No email/phone throws BadRequestException
  - Duplicate email throws ConflictException
  - Duplicate phone throws ConflictException
- `login()`: 4 tests
  - Valid credentials returns tokens
  - User not found throws UnauthorizedException
  - Inactive account throws UnauthorizedException
  - Wrong password throws UnauthorizedException
- `refreshToken()`: 4 tests
  - Valid token refreshes successfully
  - Invalid token throws UnauthorizedException
  - Wrong token type throws UnauthorizedException
  - Revoked token throws UnauthorizedException
- `logout()`: 2 tests
  - Valid token blacklists and deletes refresh token
  - Null token handled gracefully
- `logoutAll()`: 1 test
  - Revokes all user tokens
- `getCurrentUserProfile()`: 3 tests
  - Valid userId returns profile
  - Invalid UUID format throws UnauthorizedException
  - User not found throws UnauthorizedException

#### ✅ **StateServiceTest**: 9 tests
- `storeState()`: 2 tests
  - Valid input stores successfully
  - Correct prefix used
- `validateAndGetRedirectUrl()`: 6 tests
  - Valid state returns redirect URL
  - Invalid state returns null
  - Null state returns null
  - Blank state returns null
  - State deleted after validation
  - One-time use (second use returns null)
- `cleanupExpiredStates()`: 1 test
  - Executes without errors

#### ✅ **Oauth2LoginServiceTest**: 13 tests
- `findRegistration()`: 1 test
  - Invalid provider throws BadRequestException
- `upsertUser()`: 3 tests
  - Existing active user returned
  - Inactive user throws UnauthorizedException
  - New user created successfully
- `issueTokens()`: 2 tests
  - Generates access and refresh tokens
  - Stores refresh token in Redis
- OAuth Profile: 2 tests
  - Missing email validation
  - Empty name updated from profile
- Token Response: 3 tests
  - Missing token validation
  - Response with access_token valid
  - Response with id_token valid

### Test Best Practices Used
- **AAA Pattern**: Arrange, Act, Assert
- **Test Fixtures**: Static inner `TestFixtures` class for test data
- **Naming**: `methodName_scenario_expectedBehavior`
- **Assertions**: AssertJ for fluent assertions
- **Coverage**: Happy path + edge cases + error cases

### Dependencies
- AssertJ (included in `spring-boot-starter-test`)
- JUnit 5 for test framework
- Mockito for mocking
- JaCoCo for code coverage (minimum 80% target)

---

**Last Updated:** 2026-01-14  
**Maintained by:** Development Team  
**For Questions:** Check [README.md](../README.md) or Swagger docs
