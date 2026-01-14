# SPRING BOOT MICROSERVICES ECOSYSTEM - PRESENTATION CONTENT

## PHẦN 1: TỔNG QUAN & FRAMEWORK

---

### SLIDE 1: Tiêu đề dự án

**Tiêu đề chính:**
# SPRING BOOT MICROSERVICES ECOSYSTEM

**Phụ đề:**
Hệ thống Cloud-Native với DevOps hiện đại

**Thông tin:**
- Người báo cáo: [Tên của bạn]
- Ngày: 13/01/2026
- Dự án: Demo Spring Boot với Kubernetes

**Ghi chú thiết kế:**
- Nền: Gradient màu đỏ đậm (#8B0000 → #DC143C)
- Logo công ty/trường ở góc trên phải
- Font chữ lớn, bold, màu trắng
- Hiệu ứng: Shadow cho text

---

### SLIDE 2: Tech Stack sử dụng

**Tiêu đề:**
## 💻 Tech Stack Sử Dụng

**Nội dung - Chia 3 cột:**

#### Cột 1: DEVELOPMENT
- **Spring Boot 4.0.1**
  - Framework hiện đại nhất
  - Built-in Security & OAuth2
- **Java 21**
  - LTS version mới nhất
  - Virtual Threads, Pattern Matching
- **Lombok**
  - Giảm boilerplate code
  - Auto-generate getters/setters
- **Maven 3.9.9**
  - Dependency management
  - Build automation

#### Cột 2: INFRASTRUCTURE
- **MySQL 8.0.44**
  - Relational database
  - ACID compliance
- **Redis 7.4**
  - In-memory cache
  - Token storage
- **Flyway**
  - Database migration
  - Version control cho schema
- **JPA/Hibernate**
  - ORM framework
  - Entity mapping

#### Cột 3: DEVOPS & AUTOMATION
- **Docker**
  - Containerization
  - Multi-stage build
- **Kubernetes (Minikube)**
  - Container orchestration
  - Self-healing, scaling
- **GitHub Actions**
  - CI/CD pipeline
  - Automated testing
- **JaCoCo**
  - Code coverage: 80%+
  - Quality gate enforcement

**Icon cho mỗi công nghệ** (có thể tìm trên Google Images hoặc icons8.com)

**Ghi chú thiết kế:**
- Background: Đỏ đậm (#8B0000)
- Mỗi cột có border màu đỏ nhạt
- Icon công nghệ màu trắng/vàng

---

### SLIDE 3: Kiến trúc Spring Boot Core

**Tiêu đề:**
## 🏗️ Kiến Trúc Spring Boot Core

**Sơ đồ luồng dữ liệu (Data Flow Diagram):**

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Controller │────▶│   Service   │────▶│ Repository  │────▶│  Database   │
│   (@Rest)   │     │  (@Service) │     │    (JPA)    │     │   (MySQL)   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      ▲                    │                    │
      │                    │                    │
      │              ┌─────▼──────┐            │
      │              │   Entity   │            │
      │              │  (@Entity) │            │
      │              └────────────┘            │
      │                                         │
      └─────────────────────────────────────────┘
              HTTP Response (JSON)
```

**4 Khối chính:**

#### 1. Dependency Injection & IoC
- Spring Container quản lý Bean lifecycle
- @Autowired tự động inject dependencies
- Loose coupling giữa các components
- Dễ dàng testing với mock objects

#### 2. Cấu trúc phân lớp (Layered Architecture)
- **Controller**: Xử lý HTTP requests/responses
- **Service**: Business logic layer
- **Repository**: Data access layer
- **Entity**: Domain models

#### 3. RESTful API Design
- CRUD operations chuẩn REST
- HTTP Methods: GET, POST, PUT, DELETE
- JSON request/response format
- Status codes: 200, 201, 400, 401, 500

#### 4. JPA/Hibernate ORM
- Entity mapping với @Entity, @Table
- Relationships: @OneToMany, @ManyToOne
- Query methods: findBy..., save(), delete()
- Transaction management với @Transactional

**Ví dụ Code Flow:**
```
Request: POST /api/users
         ↓
AuthController.register()
         ↓
AuthService.registerUser()
         ↓
UserRepository.save()
         ↓
MySQL: INSERT INTO users
```

**Ghi chú thiết kế:**
- Sơ đồ flow ngang với mũi tên lớn
- Mỗi layer có màu sắc riêng (Controller: đỏ, Service: cam, Repository: vàng, DB: xanh)
- Animation: Từng box xuất hiện lần lượt

---

## PHẦN 2: DỮ LIỆU & BẢO MẬT

---

### SLIDE 4: Quản trị Database với Flyway

**Tiêu đề:**
## 🗄️ Quản Trị Database với Flyway

**Mô tả chính:**
Flyway = Git cho Database - Version control cho mọi thay đổi schema

**Cấu trúc Migration Files:**

```
src/main/resources/db/migration/
├── V1__Create_User_Table.sql
├── V2__Add_Role_To_Users.sql
└── V3__Add_OAuth_Support.sql (future)
```

**Ảnh so sánh: Trước và Sau Migration**

#### TRƯỚC MIGRATION (V1)
```sql
-- Không có gì, database trống
```

#### SAU V1__Create_User_Table.sql
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### SAU V2__Add_Role_To_Users.sql
```sql
ALTER TABLE users 
ADD COLUMN role VARCHAR(50) DEFAULT 'USER';

CREATE INDEX idx_users_role ON users(role);
```

**4 Lợi ích chính:**

#### 1. ✅ Tự động hóa Migration
- Không cần chạy SQL scripts thủ công
- Flyway tự động detect và apply migrations
- Rollback an toàn khi cần thiết
- **Ví dụ:** Khi app khởi động, Flyway check version hiện tại và chạy các migration còn thiếu

#### 2. 🔄 Quản lý phiên bản (Versioning)
- Mỗi migration có version number (V1, V2, V3...)
- Flyway lưu history trong table `flyway_schema_history`
- Biết chính xác database đang ở version nào
- **Query:** `SELECT * FROM flyway_schema_history;`

#### 3. 🌍 Đảm bảo tính nhất quán giữa các môi trường
- Dev, Staging, Production luôn đồng bộ
- Không lo lệch schema giữa các môi trường
- Infrastructure as Code principle
- **Scenario:** Dev thêm column mới → Staging & Production tự động apply

#### 4. 👥 Collaboration trong team
- Mỗi developer tạo migration riêng
- Git merge tự động, ít conflict
- Code review cho database changes
- **Best Practice:** Một feature = một migration file

**Flyway Lifecycle:**
```
App Start → Flyway checks → Missing migrations? → Apply → Success/Fail
                               ↓
                          No → Continue
```

**Ghi chú thiết kế:**
- Background: Đỏ burgundy (#800020)
- Table so sánh DB structure trước/sau
- Icon database với version tags
- Highlight SQL syntax

---

### SLIDE 5: Hệ thống bảo mật OAuth2 & JWT

**Tiêu đề:**
## 🔐 Hệ Thống Bảo Mật OAuth2 & JWT

**Sơ đồ chu kỳ sống của JWT:**

```
┌──────────────────────────────────────────────────────────────┐
│                    JWT AUTHENTICATION FLOW                    │
└──────────────────────────────────────────────────────────────┘

1. LOGIN REQUEST                    2. VALIDATE & GENERATE TOKEN
   ┌──────────┐                        ┌──────────────┐
   │  Client  │───POST /api/auth/login→│ AuthService  │
   │(Browser) │   {email, password}    │              │
   └──────────┘                        └───────┬──────┘
                                               │
                                               ▼
                                    ┌──────────────────┐
                                    │   JWT Service    │
                                    │ - Generate Token │
                                    │ - Sign with Key  │
                                    └────────┬─────────┘
                                             │
3. STORE IN REDIS                            ▼
   ┌─────────────────────────────────────────────┐
   │ Redis Token Store                            │
   │ Key: "token:user:123"                       │
   │ Value: {accessToken, refreshToken, expiry} │
   └─────────────────────────────────────────────┘
                    │
                    ▼
4. RETURN TO CLIENT
   ┌──────────────────────────────────────┐
   │ Response: {                          │
   │   "accessToken": "eyJhbGc...",      │
   │   "refreshToken": "eyJhbGc...",     │
   │   "expiresIn": 3600                 │
   │ }                                    │
   └──────────────────────────────────────┘
                    │
                    ▼
5. AUTHENTICATE SUBSEQUENT REQUESTS
   ┌──────────┐
   │  Client  │───GET /api/users
   │          │   Header: Authorization: Bearer <token>
   └──────────┘
        │
        ▼
   ┌────────────────────┐
   │ JwtAuthFilter      │──▶ Validate Token ──▶ Check Redis
   └────────────────────┘         │                   │
                                  ▼                   ▼
                            Valid? Yes           Token exists?
                                  │                   │
                                  ▼                   ▼
                          Allow Request         Continue
```

**4 Đặc điểm chính:**

#### 1. 🔑 Stateless Authentication
- **Khái niệm:** Server không lưu session, mọi thông tin trong token
- **Cấu trúc JWT:** Header.Payload.Signature
  ```json
  {
    "sub": "user@example.com",
    "role": "ADMIN",
    "exp": 1641024000,
    "iat": 1641020400
  }
  ```
- **Lợi ích:** Dễ scale horizontal, không cần sticky session
- **Implementation:** `JwtService.java` - Sign & verify tokens

#### 2. ⚡ Token-based Security
- **Access Token:** Thời hạn ngắn (1 giờ), dùng cho API calls
- **Refresh Token:** Thời hạn dài (7 ngày), dùng để lấy Access Token mới
- **Flow:**
  ```
  Login → Get both tokens
       ↓
  Access Token expires → Use Refresh Token
       ↓
  Get new Access Token → Continue working
  ```
- **Revocation:** Xóa token khỏi Redis = logout

#### 3. 🚀 Quản lý Token Store trên Redis
- **Tại sao dùng Redis?**
  - In-memory → Cực nhanh (< 1ms)
  - TTL tự động → Token tự xóa khi hết hạn
  - Atomic operations → Thread-safe
- **Structure:**
  ```
  Key: token:user:{userId}
  Value: {
    "accessToken": "...",
    "refreshToken": "...",
    "issuedAt": "2026-01-13T10:00:00Z",
    "expiresAt": "2026-01-13T11:00:00Z"
  }
  TTL: 3600 seconds
  ```
- **Performance:** 10,000+ token validations/second

#### 4. 🌐 OAuth2 Integration (Google, Facebook)
- **Flow:** Authorization Code Grant
  ```
  1. User clicks "Login with Google"
  2. Redirect to Google OAuth page
  3. User authorizes → Google returns code
  4. Exchange code for Google token
  5. Get user profile from Google
  6. Create/Update user in database
  7. Issue our JWT token
  ```
- **Config trong application-oauth.yaml:**
  ```yaml
  spring:
    security:
      oauth2:
        client:
          registration:
            google:
              client-id: ${GOOGLE_CLIENT_ID}
              client-secret: ${GOOGLE_CLIENT_SECRET}
              scope: profile, email
  ```
- **Implementation:** `Oauth2LoginService.java`, `OAuthController.java`

**Security Best Practices:**
- ✅ HTTPS only
- ✅ JWT Secret 256-bit minimum
- ✅ Validate token signature
- ✅ Check token expiry
- ✅ Implement token refresh mechanism
- ✅ Store sensitive data in Redis, not in JWT payload

**Ghi chú thiết kế:**
- Sơ đồ flow lớn với mũi tên rõ ràng
- Highlight Redis section với màu đỏ sáng
- Code examples trong boxes riêng
- Icons: 🔐 (lock), ⚡ (lightning), 🚀 (rocket)

---

### SLIDE 6: [DEMO 1] Verification

**Tiêu đề:**
## 🎯 [DEMO 1] Verification Process

**Mục đích:** Kiểm tra API Login, JWT generation, và data storage

---

#### PHẦN 1: API Login Test

**Công cụ:** Postman / curl / HTTPie

**Request:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "AdminPassword123!"
}
```

**Expected Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "admin@example.com",
      "fullName": "Admin",
      "role": "ADMIN"
    }
  }
}
```

**Screenshot:** Postman showing successful response

**Kiểm tra:**
- ✅ Status code 200
- ✅ AccessToken có format JWT đúng
- ✅ RefreshToken được trả về
- ✅ User info chính xác

---

#### PHẦN 2: Trích xuất & Decode JWT

**Tool:** jwt.io hoặc jwt-cli

**Decoded Access Token:**
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "admin@example.com",
    "userId": 1,
    "role": "ADMIN",
    "iat": 1705142400,
    "exp": 1705146000
  },
  "signature": "verified ✅"
}
```

**Kiểm tra:**
- ✅ Algorithm: HS256
- ✅ Subject (sub): Email của user
- ✅ Custom claims: userId, role
- ✅ Expiry (exp): 1 giờ sau issued time
- ✅ Signature valid

---

#### PHẦN 3: Kiểm tra dữ liệu trong Redis

**Truy cập Redis container:**
```bash
# Method 1: Kubernetes
kubectl exec -it redis-infra -- redis-cli

# Method 2: Docker Compose
docker exec -it redis-7 redis-cli
```

**Redis Commands:**
```redis
# List all keys
127.0.0.1:6379> KEYS *
1) "token:user:1"

# Get token details
127.0.0.1:6379> GET "token:user:1"
"{\"accessToken\":\"eyJhbG...\",\"refreshToken\":\"eyJhbG...\",\"issuedAt\":\"2026-01-13T10:00:00Z\"}"

# Check TTL (Time To Live)
127.0.0.1:6379> TTL "token:user:1"
(integer) 3456  # Seconds remaining

# Type of key
127.0.0.1:6379> TYPE "token:user:1"
string
```

**Screenshot:** Redis CLI output

**Kiểm tra:**
- ✅ Token được lưu với key đúng format
- ✅ Value chứa cả accessToken và refreshToken
- ✅ TTL tự động set (3600 seconds)

---

#### PHẦN 4: Verify MySQL Database

**Truy cập MySQL container:**
```bash
# Method 1: Kubernetes
kubectl exec -it mysql-infra -- mysql -u root -proot -D demo_db

# Method 2: Docker Compose
docker exec -it mysql-8 mysql -u root -proot -D demo_db
```

**MySQL Queries:**
```sql
-- Check user exists
mysql> SELECT * FROM users WHERE email = 'admin@example.com';
+----+---------------------+----------+------------+-------+---------------------+
| id | email               | password | full_name  | role  | created_at          |
+----+---------------------+----------+------------+-------+---------------------+
|  1 | admin@example.com  | $2a$10.. | Admin      | ADMIN | 2026-01-13 10:00:00 |
+----+---------------------+----------+------------+-------+---------------------+

-- Check password is hashed (BCrypt)
mysql> SELECT LENGTH(password) FROM users WHERE id = 1;
+------------------+
| LENGTH(password) |
+------------------+
|               60 |  # BCrypt hash always 60 chars
+------------------+

-- Check Flyway migrations
mysql> SELECT * FROM flyway_schema_history;
+-----------------+---------+---------------------+--------+----------+
| installed_rank  | version | description         | type   | success  |
+-----------------+---------+---------------------+--------+----------+
|               1 | 1       | Create User Table   | SQL    | 1        |
|               2 | 2       | Add Role To Users   | SQL    | 1        |
+-----------------+---------+---------------------+--------+----------+
```

**Screenshot:** MySQL query results

**Kiểm tra:**
- ✅ User được tạo trong database
- ✅ Password được hash (BCrypt)
- ✅ Role = ADMIN
- ✅ Flyway migrations applied successfully

---

**KẾT LUẬN DEMO 1:**
- ✅ API Login hoạt động chính xác
- ✅ JWT được generate và validate đúng
- ✅ Token được lưu trên Redis với TTL
- ✅ User data được persist trên MySQL
- ✅ Security: Password hashed, token signed

**Ghi chú thiết kế:**
- 4 sections rõ ràng với số thứ tự
- Code blocks với syntax highlighting
- Screenshots thực tế từ Postman, Redis CLI, MySQL
- Checkmarks (✅) cho mỗi verification point
- Background: Đỏ đậm với gradient

---

## PHẦN 3: DOCKER & MICROSERVICES

---

### SLIDE 7: Tư duy thiết kế Microservices

**Tiêu đề:**
## 🔧 Tư Duy Thiết Kế Microservices

**Sơ đồ so sánh (Monolith vs Microservices):**
[Chèn sơ đồ Mermaid màu sáng minh họa sự tách biệt]

**4 Lợi ích cốt lõi (Kể cả khi dùng chung DB):**
1. **Scaling:** Chỉ mở rộng dịch vụ tốn CPU/RAM cao (như Image Processing).
2. **Deployment:** Cập nhật tính năng riêng lẻ, giảm downtime hệ thống.
3. **Fault Isolation:** Lỗi ở module "Báo cáo" không làm sập module "Bán hàng".
4. **Teamwork:** Chia nhỏ code giúp nhiều team làm việc song song hiệu quả.

---

### SLIDE 7.5: Containerization với Docker

**Tiêu đề:**
## 📦 Docker: Chuẩn hóa môi trường triển khai

**Nội dung Slide (Ngắn gọn):**
- **Immutable:** Đóng gói một lần, chạy mọi nơi.
- **Isolated:** Cô lập hoàn toàn, không xung đột tài nguyên.
- **Lightweight:** Khởi động trong tích tắc, tối ưu tài nguyên.

**Lời thoại thuyết trình:**
> "Thay vì chỉ gửi code và hy vọng server có đủ môi trường phù hợp, Docker cho phép ta gửi đi **toàn bộ hệ thống đã đóng gói**. Nó giống như một thùng Container tiêu chuẩn: Dù hạ tầng bên dưới là gì, chỉ cần có Docker, ứng dụng sẽ chạy chính xác 100% như trên máy dev, loại bỏ hoàn toàn lỗi khác biệt môi trường."

---

### SLIDE 8: Tối ưu hóa Docker (Dev & Multi-stage Build)

**Tiêu đề:**
## 🐳 Tối ưu hóa Docker: Từ Development đến Production

**1. Tối ưu Phát triển (Dev Experience):**
- **Dev Container:** Đồng bộ công cụ lập trình cho cả team.
- **Spring DevTools:** Hot-reload ứng dụng ngay trong Container.

**2. Tối ưu Vận hành (Multi-stage Build):**
- **Stage 1 (Build):** Dùng Maven + JDK để biên dịch và đóng gói file JAR.
- **Stage 2 (Runtime):** Chỉ dùng JRE siêu nhẹ để chạy JAR, loại bỏ toàn bộ source code và công cụ build.

**Kết quả:**
- **Kích thước Image:** Giảm từ ~800MB → ~250MB.
- **Bảo mật:** Giảm thiểu diện tích tấn công (Attack Surface).

**Lời thoại thuyết trình:**
> "Để tối ưu hệ thống, chúng tôi chia quá trình build thành 2 giai đoạn. **Stage 1** là nơi làm việc nặng nhọc với JDK và Maven để tạo ra file JAR. Ngay sau đó, ở **Stage 2**, chúng tôi chỉ lấy duy nhất file JAR đó đặt vào một môi trường JRE siêu nhẹ. Kết quả là một Image cực kỳ tinh gọn, giúp việc triển khai nhanh hơn và an toàn hơn rất nhiều."

```dockerfile
# ============================================
# STAGE 1: BUILD (Builder stage)
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy pom.xml và download dependencies trước (layer caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline

# Copy source code và build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Kết quả: app.jar trong /app/target/

# ============================================
# STAGE 2: RUNTIME (Final stage)
# ============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy ONLY jar file từ build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

**Bảng so sánh kích thước Image:**

| Approach | Base Image | Includes | Image Size | Build Time |
|----------|-----------|----------|------------|------------|
| **Single-stage** | `eclipse-temurin:21-jdk` | JDK (180MB)<br/>Maven (80MB)<br/>Source code (50MB)<br/>Dependencies (200MB)<br/>JAR file (60MB) | **~800MB** | 3-4 phút |
| **Multi-stage** | Stage 1: `21-jdk` (build)<br/>Stage 2: `21-jre` (runtime) | JRE (50MB)<br/>JAR file (60MB)<br/>*Maven & source bị loại bỏ* | **~250MB