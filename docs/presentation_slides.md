# SPRING BOOT MICROSERVICES ECOSYSTEM - SLIDES CONTENT

## SLIDE 1: TIÊU ĐỀ DỰ ÁN
- **Tiêu đề chính:** SPRING BOOT MICROSERVICES ECOSYSTEM
- **Phụ đề:** Hệ thống Cloud-Native với DevOps hiện đại
- **Thông tin:**
  - Người báo cáo: [Tên của bạn]
  - Ngày: 13/01/2026
  - Dự án: Demo Spring Boot với Kubernetes

---

## SLIDE 2: TECH STACK SỬ DỤNG
| DEVELOPMENT | INFRASTRUCTURE | DEVOPS & AUTOMATION |
| :--- | :--- | :--- |
| **Spring Boot 4.0.1** | **MySQL 8.0.44** | **Docker** (Containerization) |
| **Java 21 (LTS)** | **Redis 7.4** (Cache/Token) | **Kubernetes** (Orchestration) |
| **Lombok** | **Flyway** (Migration) | **GitHub Actions** (CI/CD) |
| **Maven 3.9.9** | **JPA/Hibernate** | **JaCoCo** (Coverage 80%+) |

---

## SLIDE 3: KIẾN TRÚC SPRING BOOT CORE
- **Mô hình Phân Lớp:** Controller → Service → Repository → Database
- **Dependency Injection:** Spring IoC quản lý Bean lifecycle, giúp Loose Coupling.
- **RESTful API:** Tuân thủ chuẩn CRUD, HTTP Methods (GET, POST, PUT, DELETE).
- **ORM:** JPA/Hibernate tự động ánh xạ Java Objects xuống Database.

---

## SLIDE 4: QUẢN TRỊ DATABASE VỚI FLYWAY
- **Khái niệm:** Flyway là "Git cho Database", quản lý phiên bản schema.
- **Quy trình:**
  1. Tạo file migration: `V1__Create_User_Table.sql`.
  2. Flyway tự động apply khi App khởi động.
  3. Lưu lịch sử trong table `flyway_schema_history`.
- **Lợi ích:** Đồng bộ schema giữa các môi trường (Dev, Staging, Production).

---

## SLIDE 5: HỆ THỐNG BẢO MẬT OAUTH2 & JWT
- **Cơ chế:** Stateless Authentication.
- **Luồng hoạt động:** 
  1. User Login → Server xác thực.
  2. Server phát hành **JWT** (JSON Web Token).
  3. Client đính kèm Token vào Header cho các request sau.
- **Bảo mật:** Password được mã hóa bằng **BCrypt**, Token được lưu trên **Redis** với thời gian hết hạn (TTL).

---

## SLIDE 6: TƯ DUY THIẾT KẾ MICROSERVICES
- **Monolithic:** Một khối duy nhất, khó scale, một lỗi làm sập toàn bộ.
- **Microservices:**
  - Chia tách theo nghiệp vụ (**Domain-Driven Design**).
  - **Database per Service:** Mỗi dịch vụ quản lý dữ liệu riêng.
  - Giao tiếp qua **REST API**.
- **Lợi ích:** Scale độc lập, cô lập lỗi, triển khai CI/CD linh hoạt.

---

## SLIDE 7: DOCKER MULTI-STAGE BUILD
- **Vấn đề:** Docker image thông thường chứa cả JDK, Maven, Source code (~800MB).
- **Giải pháp:** Tách giai đoạn **BUILD** (dùng JDK) và **RUNTIME** (chỉ dùng JRE).
- **Kết quả:** Giảm kích thước image xuống còn **~250MB**, bảo mật hơn vì không chứa mã nguồn.

---

## SLIDE 8: KUBERNETES & CI/CD GITHUB ACTIONS
- **CI/CD:** Tự động Build → Test (JaCoCo) → Push Docker Image.
- **Kubernetes (K8s):**
  - Quản lý Container tự động.
  - **Self-healing:** Tự khởi động lại khi container lỗi.
  - **Scaling:** Tự động tăng/giảm tài nguyên theo lưu lượng.
