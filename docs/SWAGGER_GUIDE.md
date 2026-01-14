# API Documentation with Swagger/OpenAPI

Dự án sử dụng `springdoc-openapi` để tự động tạo tài liệu API dựa trên các Controller và DTO.

## 1. Cấu hình
Thư viện đã được thêm vào `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.3</version>
</dependency>
```

## 2. Cách truy cập
Sau khi khởi chạy ứng dụng, bạn có thể truy cập các đường dẫn sau:

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
  - Giao diện trực quan để xem và test các API endpoint.
- **OpenAPI Spec (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
  - Đặc tả API dưới dạng JSON để tích hợp với các công cụ khác (như Postman).

## 3. Các Annotation cơ bản
Sử dụng các annotation sau để làm rõ tài liệu API:
- `@Tag(name = "...", description = "...")`: Phân nhóm API (thường đặt ở Controller).
- `@Operation(summary = "...", description = "...")`: Mô tả chi tiết cho từng endpoint.
- `@ApiResponse`: Định nghĩa các mã phản hồi (200, 400, 401, 500).
- `@Schema`: Mô tả các thuộc tính trong DTO.

## 5. API Endpoints Documentation

### 5.1. Authentication APIs
**Base Path:** `/api/auth`

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/register` | POST | Đăng ký tài khoản mới | No |
| `/login` | POST | Đăng nhập | No |
| `/refresh` | POST | Làm mới access token | No |
| `/logout` | POST | Đăng xuất | Yes |
| `/logout-all` | POST | Đăng xuất tất cả thiết bị | Yes |
| `/me` | GET | Lấy thông tin user hiện tại | Yes |

### 5.2. User Management APIs
**Base Path:** `/api/users`

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/profile` | PUT | Cập nhật thông tin cá nhân | Yes |
| `/profile/delete` | POST | Xóa tài khoản (soft delete) | Yes |

#### PUT `/api/users/profile` - Update Profile
**Request Body:**
```json
{
  "fullName": "Nguyen Van A",
  "email": "newmail@example.com",
  "phone": "0987654321"
}
```

**Response (200 OK):**
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

#### POST `/api/users/profile/delete` - Delete Account
> ⚠️ **Lưu ý:** Sử dụng POST thay vì DELETE vì cần gửi body (password xác nhận). DELETE với body không được nhiều HTTP client/proxy hỗ trợ.

**Request Body:**
```json
{
  "password": "current_password",
  "reason": "Không còn sử dụng"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Tài khoản đã được xóa thành công",
  "data": null
}
```

## 6. Testing with Swagger UI

### Step 1: Start Application
```bash
docker compose up
```

### Step 2: Access Swagger UI
Open [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### Step 3: Authenticate
1. Click **"Authorize"** button (top right)
2. Login to get access token:
   - Use `/api/auth/login` endpoint
   - Or `/api/auth/register` to create new account
3. Copy the `accessToken` from response
4. Paste in Authorization popup: `Bearer <your_access_token>`
5. Click **"Authorize"**

### Step 4: Test User Management APIs
1. Go to **"User Management"** section
2. Try **PUT `/api/users/profile`**:
   - Click "Try it out"
   - Modify request body (fullName, email, phone)
   - Click "Execute"
3. Try **POST `/api/users/profile/delete`**:
   - Click "Try it out"
   - Provide your password and optional reason
   - Click "Execute"

## 7. Tích hợp trong CI/CD
Trong quy trình CI/CD ([ci.yml](file:///d:/demo/.github/workflows/ci.yml)), khi lệnh `./mvnw clean verify` chạy:
- Các định nghĩa OpenAPI sẽ được kiểm tra tính hợp lệ.
- Tài liệu API luôn được cập nhật tương ứng với phiên bản code mới nhất.

## 8. Lưu ý cho K8S
Khi deploy lên Kubernetes, hãy đảm bảo cấu hình `Service` và `Ingress` cho phép truy cập port `8080` để xem Swagger UI từ bên ngoài nếu cần thiết.
