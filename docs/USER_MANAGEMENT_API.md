# User Management API Design

## Tổng quan
API quản lý thông tin người dùng: cập nhật thông tin cá nhân và xóa tài khoản.

---

## 1. API Update User Profile (Cập nhật thông tin người dùng)

### Endpoint
```
PUT /api/users/profile
```

### Mô tả
- Người dùng đã đăng nhập có thể cập nhật thông tin cá nhân của mình
- Chỉ cập nhật được: `fullName`, `email`, `phone`
- Không thể thay đổi: `id`, `role`, `createdAt`, `password` (có API riêng)

### Authentication
- **Required**: Bearer Token (Access Token)
- User chỉ có thể cập nhật thông tin của chính mình

### Request Headers
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Request Body (DTO: UpdateProfileRequest)
```json
{
  "fullName": "Nguyen Van A",
  "email": "newmail@example.com",   // optional
  "phone": "0987654321"              // optional
}
```

**Validation Rules:**
- `fullName`: required, min=2, max=100
- `email`: optional, valid email format, unique
- `phone`: optional, valid phone format (Vietnamese: 10 digits, start with 0), unique

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Cập nhật thông tin thành công",
  "data": {
    "id": "uuid",
    "fullName": "Nguyen Van A",
    "email": "newmail@example.com",
    "phone": "0987654321",
    "role": "USER",
    "active": true,
    "createdAt": "2026-01-14T10:00:00",
    "updatedAt": "2026-01-14T12:00:00"
  }
}
```

### Error Responses

**400 Bad Request** - Validation errors
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "email": "Email đã được sử dụng bởi tài khoản khác",
    "phone": "Số điện thoại không hợp lệ"
  }
}
```

**401 Unauthorized** - Token invalid/expired
```json
{
  "success": false,
  "message": "Vui lòng đăng nhập để thực hiện thao tác này",
  "code": "UNAUTHORIZED"
}
```

**404 Not Found** - User not found
```json
{
  "success": false,
  "message": "Không tìm thấy thông tin người dùng"
}
```

---

## 2. API Delete User Account (Xóa tài khoản)

### Endpoint
```
DELETE /api/users/profile
```

### Mô tả
- Người dùng có thể xóa tài khoản của chính mình
- **Soft delete**: Set `active = false` thay vì xóa vật lý
- Tất cả refresh tokens của user sẽ bị thu hồi (blacklist)

### Authentication
- **Required**: Bearer Token (Access Token)
- User chỉ có thể xóa tài khoản của chính mình

### Request Headers
```
Authorization: Bearer <access_token>
```

### Request Body (Optional: DTO: DeleteAccountRequest)
```json
{
  "password": "current_password",  // Xác nhận mật khẩu để đảm bảo an toàn
  "reason": "Không còn sử dụng"    // Optional: Lý do xóa tài khoản
}
```

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Tài khoản đã được xóa thành công"
}
```

### Error Responses

**400 Bad Request** - Wrong password
```json
{
  "success": false,
  "message": "Mật khẩu không chính xác"
}
```

**401 Unauthorized** - Token invalid/expired
```json
{
  "success": false,
  "message": "Vui lòng đăng nhập để thực hiện thao tác này",
  "code": "UNAUTHORIZED"
}
```

**403 Forbidden** - Cannot delete admin account
```json
{
  "success": false,
  "message": "Không thể xóa tài khoản quản trị viên"
}
```

---

## 3. Implementation Plan

### 3.1. DTOs (Data Transfer Objects)

#### `UpdateProfileRequest.java`
```java
@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
```

#### `DeleteAccountRequest.java`
```java
@Data
public class DeleteAccountRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu để xác nhận")
    private String password;

    private String reason; // Optional
}
```

#### `UserProfileResponse.java`
```java
@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 3.2. Controller - UserController.java

**Location:** `src/main/java/com/baohoanhao/demo/controller/UserController.java`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Quản lý thông tin người dùng")
public class UserController {
    
    private final UserService userService;

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        UserProfileResponse response = userService.updateProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", response));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Xóa tài khoản")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        userService.deleteAccount(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được xóa thành công"));
    }
}
```

---

### 3.3. Service - UserService.java

**Location:** `src/main/java/com/baohoanhao/demo/service/UserService.java`

**Methods to implement:**

1. **updateProfile(String userId, UpdateProfileRequest request)**
   - Validate userId format (UUID)
   - Check if user exists
   - Check email uniqueness (nếu thay đổi email)
   - Check phone uniqueness (nếu thay đổi phone)
   - Update user entity
   - Return UserProfileResponse

2. **deleteAccount(String userId, DeleteAccountRequest request)**
   - Validate userId format
   - Get user from DB
   - Verify password matches
   - Check if user is ADMIN → throw exception (không cho xóa admin)
   - Set `active = false`
   - Revoke all refresh tokens (call TokenStorageService.revokeAllUserTokens)
   - Save user

---

### 3.4. Security Configuration

**SecurityConfig.java** - Endpoints đã được bảo vệ:
```java
// Tất cả /api/users/** đều yêu cầu authentication (default)
.anyRequest().authenticated()
```

✅ Không cần thêm config, vì `/api/users/**` đã yêu cầu authentication.

---

### 3.5. Exception Handling

Sử dụng các exceptions hiện có:
- `BadRequestException` - Validation errors, duplicate email/phone
- `UnauthorizedException` - Token invalid
- `NotFoundException` - User not found
- `ForbiddenException` (new) - Cannot delete admin account

**Create:** `ForbiddenException.java`
```java
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
```

---

### 3.6. Testing Plan

#### Unit Tests (UserServiceTest.java)
- ✅ Update profile successfully
- ✅ Update email with duplicate email → BadRequestException
- ✅ Update phone with duplicate phone → BadRequestException
- ✅ Update profile with invalid userId → NotFoundException
- ✅ Delete account successfully
- ✅ Delete account with wrong password → BadRequestException
- ✅ Delete admin account → ForbiddenException

#### Integration Tests (UserControllerTest.java)
- ✅ PUT /api/users/profile returns 200 with valid token
- ✅ PUT /api/users/profile returns 401 without token
- ✅ DELETE /api/users/profile returns 200 with valid password
- ✅ DELETE /api/users/profile returns 403 for admin user

---

## 4. Database Impact

**No migration needed** - Sử dụng các cột hiện có:
- `users.full_name`
- `users.email`
- `users.phone`
- `users.active` (soft delete)
- `users.updated_at` (auto-updated by @UpdateTimestamp)

---

## 5. Redis Impact

**TokenStorageService** cần thêm method:
```java
public void revokeAllUserTokens(String userId) {
    // Lấy tất cả refresh tokens của user từ Redis
    // Thêm vào blacklist
    // Xóa khỏi active tokens
}
```

---

## 6. Swagger Documentation

Sau khi implement, test API tại:
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **Section:** User Management
- **Try it out** với Bearer token

---

## 7. Checklist Implementation

- [x] Create DTOs: `UpdateProfileRequest`, `DeleteAccountRequest`, `UserProfileResponse`
- [x] Create `ForbiddenException`
- [x] Create `UserController` with 2 endpoints
- [x] Implement `UserService.updateProfile()`
- [x] Implement `UserService.deleteAccount()`
- [x] Add `TokenStorageService.revokeAllUserTokens()` (Already exists)
- [x] Write Unit Tests
- [x] Write Integration Tests
- [x] Update SWAGGER_GUIDE.md with new endpoints
- [ ] Test with Swagger UI (Manual testing by user)

---

## 8. Security Considerations

✅ **Authorization:** User chỉ có thể update/delete chính mình (check `authentication.getName()` == `userId`)
✅ **Password confirmation:** Khi xóa tài khoản phải nhập lại password
✅ **Soft delete:** Không xóa vật lý, giữ lại data cho audit
✅ **Token revocation:** Revoke tất cả refresh tokens khi xóa tài khoản
✅ **Admin protection:** Không cho phép xóa tài khoản ADMIN

---

## Tổng kết

Kế hoạch hoàn chỉnh để implement 2 API:
1. **PUT /api/users/profile** - Update thông tin
2. **DELETE /api/users/profile** - Xóa tài khoản (soft delete)

Có thể bắt đầu implement theo thứ tự:
1. DTOs
2. Exception
3. Service
4. Controller
5. Tests

---

## 9. Frontend Integration Guide

### Base URL
```
http://localhost:8080/api/users
```

### Example: Update Profile (JavaScript/TypeScript)
```javascript
const updateProfile = async (profileData) => {
  const response = await fetch('http://localhost:8080/api/users/profile', {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fullName: profileData.fullName,
      email: profileData.email,      // optional
      phone: profileData.phone        // optional
    })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return await response.json(); // { success: true, message: "...", data: {...} }
};
```

### Example: Delete Account
```javascript
const deleteAccount = async (password, reason) => {
  const response = await fetch('http://localhost:8080/api/users/profile', {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      password: password,
      reason: reason || undefined  // optional
    })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  // After successful deletion, clear tokens and redirect to login
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  window.location.href = '/login';
};
```

### TypeScript Types (Optional)
```typescript
interface UpdateProfileRequest {
  fullName: string;
  email?: string;
  phone?: string;
}

interface UserProfileResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  role: 'USER' | 'ADMIN';
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  errors?: Record<string, string>;
}
```

### Error Handling Strategy
```javascript
try {
  await updateProfile(data);
  // Show success message
} catch (error) {
  // Handle specific errors based on status code
  if (error.response?.status === 400) {
    // Show validation errors from error.errors object
  } else if (error.response?.status === 401) {
    // Redirect to login (token expired)
  } else {
    // Show generic error message
  }
}
```
