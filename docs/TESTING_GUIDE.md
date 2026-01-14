# 📘 Hướng Dẫn Unit Testing cho API

## 📋 Mục lục
- [Giới thiệu](#giới-thiệu)
- [Cài đặt và cấu hình](#cài-đặt-và-cấu-hình)
- [Kiến trúc Testing](#kiến-trúc-testing)
- [Các loại Test](#các-loại-test)
- [Viết Unit Test cho Controller](#viết-unit-test-cho-controller)
- [Viết Unit Test cho Service](#viết-unit-test-cho-service)
- [Best Practices](#best-practices)
- [Chạy Tests](#chạy-tests)
- [Code Coverage](#code-coverage)

---

## 🎯 Giới thiệu

Tài liệu này hướng dẫn cách viết và chạy unit test cho các API trong dự án Spring Boot. Dự án sử dụng:
- **JUnit 5** (Jupiter) - Framework testing
- **Mockito** - Mocking framework
- **MockMvc** - Testing Spring MVC controllers
- **Spring Boot Test** - Auto-configuration cho tests

## ⚙️ Cài đặt và cấu hình

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Test Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Maven Surefire cho Unit Tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0</version>
        </plugin>
        
        <!-- JaCoCo cho Code Coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
        </plugin>
    </plugins>
</build>
```

---

## 🏗️ Kiến trúc Testing

### Cấu trúc thư mục

```
src/
├── main/
│   └── java/
│       └── com/baohoanhao/demo/
│           ├── controller/
│           │   ├── AuthController.java
│           │   └── UserController.java
│           └── service/
│               ├── AuthService.java
│               └── UserService.java
└── test/
    └── java/
        └── com/baohoanhao/demo/
            ├── controller/
            │   ├── AuthControllerTest.java
            │   └── UserControllerTest.java
            └── service/
                ├── AuthServiceTest.java
                └── UserServiceTest.java
```

### Naming Convention

- **Test class**: `{ClassName}Test.java`
- **Test method**: `{methodName}_{scenario}_Should{expectedBehavior}`

**Ví dụ:**
```java
@Test
void updateProfile_ValidRequest_ShouldReturnUpdatedProfile() { }

@Test
void deleteAccount_InvalidPassword_ShouldThrowBadRequestException() { }
```

---

## 🔍 Các loại Test

### 1. **Controller Test** (Integration Test với MockMvc)

Test các REST API endpoints, request/response, HTTP status codes.

**Đặc điểm:**
- Sử dụng `@SpringBootTest` và `@AutoConfigureMockMvc`
- Mock các service dependencies bằng `@MockBean`
- Test HTTP request/response với `MockMvc`

### 2. **Service Test** (Unit Test)

Test business logic trong service layer.

**Đặc điểm:**
- Sử dụng `@ExtendWith(MockitoExtension.class)`
- Mock repositories và dependencies
- Test logic thuần túy không cần Spring context

---

## 🧪 Viết Unit Test cho Controller

### Template cơ bản

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserProfileResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Setup mock data
        mockResponse = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void updateProfile_Success() throws Exception {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        
        when(userService.updateProfile(any(), any()))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Test User"));

        verify(userService, times(1)).updateProfile(any(), any());
    }
}
```

### Các annotations quan trọng

| Annotation | Mô tả |
|-----------|-------|
| `@SpringBootTest` | Load full Spring context |
| `@AutoConfigureMockMvc` | Auto-config MockMvc |
| `@MockBean` | Mock Spring bean |
| `@WithMockUser` | Giả lập user đã authenticated |
| `@BeforeEach` | Chạy trước mỗi test |

### Test các scenarios

#### ✅ Test Success Case

```java
@Test
@WithMockUser(username = "user-123")
void updateProfile_ValidRequest_ShouldReturnSuccess() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("John Doe");
    request.setEmail("john@example.com");

    when(userService.updateProfile(any(), any()))
            .thenReturn(mockResponse);

    mockMvc.perform(put("/api/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
}
```

#### ❌ Test Validation Error

```java
@Test
@WithMockUser
void updateProfile_InvalidEmail_ShouldReturn400() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setEmail("invalid-email");  // Email không hợp lệ

    mockMvc.perform(put("/api/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));

    verify(userService, never()).updateProfile(any(), any());
}
```

#### 🔒 Test Unauthorized

```java
@Test
void updateProfile_NoAuthentication_ShouldReturn401() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setFullName("Test");

    mockMvc.perform(put("/api/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
}
```

#### 💥 Test Exception Handling

```java
@Test
@WithMockUser
void deleteAccount_InvalidPassword_ShouldReturn400() throws Exception {
    DeleteAccountRequest request = new DeleteAccountRequest();
    request.setPassword("wrong-password");

    doThrow(new BadRequestException("Mật khẩu không chính xác"))
            .when(userService).deleteAccount(any(), any());

    mockMvc.perform(post("/api/users/profile/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Mật khẩu không chính xác"));
}
```

---

## 🔧 Viết Unit Test cho Service

### Template cơ bản

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        testUser = new User();
        testUser.setId(UUID.fromString(userId));
        testUser.setEmail("test@example.com");
    }

    @Test
    void updateProfile_Success() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setEmail("new@example.com");

        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(testUser);

        // Act
        UserProfileResponse response = userService.updateProfile(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals("New Name", testUser.getFullName());
        verify(userRepository, times(1)).save(any());
    }
}
```

### Các annotations quan trọng

| Annotation | Mô tả |
|-----------|-------|
| `@ExtendWith(MockitoExtension.class)` | Enable Mockito |
| `@Mock` | Tạo mock object |
| `@InjectMocks` | Inject mocks vào class đang test |
| `@BeforeEach` | Setup trước mỗi test |

### Test các scenarios

#### ✅ Test Success Case

```java
@Test
void updateProfile_ValidData_ShouldUpdateSuccessfully() {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setEmail("updated@example.com");
    request.setFullName("Updated Name");

    when(userRepository.findById(UUID.fromString(userId)))
            .thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail(request.getEmail()))
            .thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    UserProfileResponse response = userService.updateProfile(userId, request);

    assertNotNull(response);
    assertEquals("Updated Name", testUser.getFullName());
    assertEquals("updated@example.com", testUser.getEmail());
}
```

#### ❌ Test User Not Found

```java
@Test
void updateProfile_UserNotFound_ShouldThrowException() {
    UpdateProfileRequest request = new UpdateProfileRequest();
    
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, 
            () -> userService.updateProfile(userId, request));
    
    verify(userRepository, never()).save(any());
}
```

#### ⚠️ Test Email Already Exists

```java
@Test
void updateProfile_EmailExists_ShouldThrowBadRequestException() {
    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setEmail("existing@example.com");

    when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

    assertThrows(BadRequestException.class,
            () -> userService.updateProfile(userId, request));

    verify(userRepository, never()).save(any());
}
```

#### 🔐 Test Password Validation

```java
@Test
void deleteAccount_WrongPassword_ShouldThrowException() {
    DeleteAccountRequest request = new DeleteAccountRequest();
    request.setPassword("wrong-password");

    when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    assertThrows(BadRequestException.class,
            () -> userService.deleteAccount(userId, request));

    verify(userRepository, never()).save(any());
}
```

---

## ✨ Best Practices

### 1. **Naming Convention**

```java
// ✅ GOOD - Descriptive
@Test
void updateProfile_EmailAlreadyExists_ShouldThrowBadRequestException()

// ❌ BAD - Vague
@Test
void testUpdate()
```

### 2. **AAA Pattern (Arrange-Act-Assert)**

```java
@Test
void example() {
    // Arrange - Setup test data
    UpdateProfileRequest request = new UpdateProfileRequest();
    when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
    
    // Act - Execute method
    UserProfileResponse response = userService.updateProfile(userId, request);
    
    // Assert - Verify results
    assertNotNull(response);
    verify(userRepository, times(1)).save(any());
}
```

### 3. **Test Isolation**

Mỗi test phải độc lập, không phụ thuộc vào test khác:

```java
@BeforeEach
void setUp() {
    // Reset data trước mỗi test
    testUser = new User();
    testUser.setId(UUID.randomUUID());
}
```

### 4. **Mock Only External Dependencies**

```java
// ✅ GOOD - Mock repository (external dependency)
@Mock
private UserRepository userRepository;

// ❌ BAD - Không mock class đang test
@Mock
private UserService userService;  // Wrong!
```

### 5. **Verify Interactions**

```java
@Test
void updateProfile_Success() {
    // ...
    userService.updateProfile(userId, request);
    
    // Verify method được gọi đúng số lần
    verify(userRepository, times(1)).save(any());
    verify(userRepository, never()).delete(any());
}
```

### 6. **Test Edge Cases**

```java
@Test
void updateProfile_NullEmail_ShouldUseExistingEmail() { }

@Test
void updateProfile_EmptyFullName_ShouldThrowValidationException() { }

@Test
void deleteAccount_AdminRole_ShouldThrowForbiddenException() { }
```

### 7. **Organize Tests với @Nested**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {
        @Test
        void login_ValidCredentials_ShouldReturnToken() { }
        
        @Test
        void login_InvalidPassword_ShouldReturn401() { }
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {
        @Test
        void register_ValidData_ShouldCreateUser() { }
    }
}
```

---

## 🚀 Chạy Tests

### 1. Chạy tất cả tests

```bash
mvn test
```

### 2. Chạy test một class cụ thể

```bash
mvn test -Dtest=UserServiceTest
```

### 3. Chạy test một method cụ thể

```bash
mvn test -Dtest=UserServiceTest#updateProfile_Success
```

### 4. Chạy tests và skip build

```bash
mvn test -DskipTests=false
```

### 5. Chạy tests với logging

```bash
mvn test -X
```

---

## 📊 Code Coverage

### Current Test Coverage (2026-01-14)

**Total: 57 unit tests** covering all 4 services

#### ✅ Service Layer Coverage

| Service | Tests | Coverage | Status |
|---------|-------|----------|--------|
| **UserService** | 16 tests | ~95% | ✅ Excellent |
| **AuthService** | 19 tests | ~90% | ✅ Excellent |
| **StateService** | 9 tests | ~85% | ✅ Good |
| **Oauth2LoginService** | 13 tests | ~60% | ⚠️ Needs improvement |

#### Test Breakdown

**UserServiceTest (16 tests)**
- `updateProfile()`: 8 tests
  - Valid full name update
  - Unique email update
  - Duplicate email throws BadRequestException
  - Same email skips uniqueness check
  - Unique phone update
  - Duplicate phone throws BadRequestException
  - Same phone skips uniqueness check
  - User not found throws ResourceNotFoundException
- `deleteAccount()`: 6 tests
  - Valid password deactivates account
  - Revokes all tokens on deletion
  - Incorrect password throws BadRequestException
  - Admin user throws ForbiddenException
  - User not found throws ResourceNotFoundException
  - Invalid UUID throws BadRequestException

**AuthServiceTest (19 tests)**
- `register()`: 5 tests (email, phone, validation, duplicates)
- `login()`: 4 tests (valid, not found, inactive, wrong password)
- `refreshToken()`: 4 tests (valid, invalid, wrong type, revoked)
- `logout()`: 2 tests (valid token, null token)
- `logoutAll()`: 1 test
- `getCurrentUserProfile()`: 3 tests (valid, invalid UUID, not found)

**StateServiceTest (9 tests)**
- `storeState()`: 2 tests (valid input, prefix check)
- `validateAndGetRedirectUrl()`: 6 tests
- `cleanupExpiredStates()`: 1 test

**Oauth2LoginServiceTest (13 tests)**
- OAuth2 provider validation
- User upsert logic
- Token generation and storage
- Profile handling

### Test Best Practices Implemented

✅ **AAA Pattern**: All tests follow Arrange-Act-Assert structure  
✅ **Test Fixtures**: Use static inner `TestFixtures` class for reusable test data  
✅ **Naming Convention**: `methodName_scenario_expectedBehavior`  
✅ **Assertions**: AssertJ for fluent, readable assertions  
✅ **Coverage**: Happy path + edge cases + error cases  
✅ **Isolation**: Each test is independent with `@BeforeEach` setup  
✅ **Lenient Mocking**: `@MockitoSettings(strictness = Strictness.LENIENT)` where needed

### Viewing Coverage Report

#### Option 1: IntelliJ IDEA (Recommended)
```
1. Right-click on test folder/file
2. Select "Run with Coverage"
3. View inline coverage in editor (green/red highlights)
4. Generate HTML report from Coverage tab
```

#### Option 2: Maven Command Line
```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html  # macOS/Linux
start target\site\jacoco\index.html  # Windows
```

#### Option 3: Windows Command (Maven Wrapper)
```bash
.\mvnw clean test jacoco:report
start target\site\jacoco\index.html
```

### Coverage Metrics Explained

| Metric | Description | Target |
|--------|-------------|--------|
| **Line Coverage** | % of code lines executed | 80%+ |
| **Branch Coverage** | % of if/else branches tested | 75%+ |
| **Method Coverage** | % of methods called | 85%+ |
| **Class Coverage** | % of classes with tests | 100% |

### Coverage Configuration (pom.xml)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum> <!-- 80% coverage -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

---

## 📈 Coverage Goals

| Layer | Target Coverage |
|-------|-----------------|
| Service | 90%+ |
| Controller | 80%+ |
| Repository | 70%+ |
| Overall | 80%+ |

---

## 🔗 Tài liệu tham khảo

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)

---

## 📝 Ví dụ Hoàn Chỉnh

Xem các file test mẫu:
- [UserControllerTest.java](../src/test/java/com/baohoanhao/demo/controller/UserControllerTest.java)
- [UserServiceTest.java](../src/test/java/com/baohoanhao/demo/service/UserServiceTest.java)
- [AuthControllerTest.java](../src/test/java/com/baohoanhao/demo/controller/AuthControllerTest.java)

---

**Được tạo bởi:** BaoHoanHao  
**Cập nhật lần cuối:** 2026-01-14
