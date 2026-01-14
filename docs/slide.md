# CHI TIẾT NỘI DUNG SLIDE THUYẾT TRÌNH

## PHẦN 1: TỔNG QUAN & FRAMEWORK

### Slide 1: Tiêu đề dự án
- **Nội dung:**
    - Tên dự án: **SPRING BOOT MICROSERVICES ECOSYSTEM**
    - Phụ đề: Hệ thống Cloud-Native với DevOps hiện đại.
    - Người báo cáo: [Tên của bạn]
    - Ngày tháng: 13/01/2026
- **Bố cục:** Chữ trắng/vàng trên nền đỏ đậm gradient, logo ở góc trên bên phải, font chữ bold mạnh mẽ.

### Slide 2: Tech Stack sử dụng
- **Nội dung:**
    - **Development:** Java 21 LTS, Spring Boot 4.0.1, Lombok, Maven 3.9.9.
    - **Infrastructure:** MySQL 8.0.44 (Relational), Redis 7.4 (Cache/Token), Flyway (DB Version Control).
    - **DevOps:** Docker (Multi-stage), Kubernetes (Minikube), GitHub Actions (CI/CD), JaCoCo (Coverage).
- **Bố cục:** Chia 3 cột rõ ràng, mỗi cột có tiêu đề và danh sách icon công nghệ tương ứng.

### Slide 3: Kiến trúc Spring Boot Core
- **Nội dung:**
    - **Cấu trúc phân lớp:** Controller (@Rest) → Service (@Service) → Repository (JPA) → Entity (@Entity).
    - **Nguyên lý:** Dependency Injection (IoC) giúp giảm sự phụ thuộc, dễ dàng viết Unit Test với Mockito.
    - **Data Flow:** HTTP Request → Controller xử lý routing → Service thực thi logic → Repo truy vấn DB.
- **Bố cục:** Sơ đồ luồng ngang với các mũi tên màu sắc khác nhau cho từng layer.

---

## PHẦN 2: DỮ LIỆU & BẢO MẬT

### Slide 4: Quản trị Dữ liệu với JPA, Hibernate & Flyway
- **Nội dung:**
    - **JPA & Hibernate:** Tự động ánh xạ Object-Relational (ORM), giúp thao tác với database qua thực thể (Entity) thay vì SQL thuần.
    - **Flyway Integration:** Điểm nhấn quan trọng giúp quản lý phiên bản database đồng bộ với code (V1, V2...).
    - **Sự kết hợp:** Hibernate định nghĩa cấu trúc dữ liệu → Flyway đảm bảo tính nhất quán giữa các môi trường (Dev/Staging/Prod).
- **Bố cục:** Sơ đồ kết hợp giữa Entity -> Hibernate -> Flyway -> MySQL. Kèm ảnh chụp bảng `flyway_schema_history`.

### Slide 5: Hệ thống bảo mật OAuth2 & JWT
- **Nội dung:**
    - **Cơ chế:** Stateless (không lưu session trên server). Sử dụng JWT để xác thực và phân quyền.
    - **Quản lý Token:** Token được ký bằng Secret Key, lưu trữ trên Redis với TTL (Time-to-live) để tối ưu hiệu năng.
    - **Bảo mật:** Password được băm bằng BCrypt trước khi lưu vào MySQL.
- **Bố cục:** Sơ đồ 3 bước: 1. Login → 2. Issue JWT → 3. Access with JWT.

### Slide 6: [DEMO 1] Verification
- **Nội dung:**
    - Thực hiện Login qua Postman → Nhận mã 200 OK và chuỗi JWT.
    - Dùng JWT truy cập API `/api/users/profile`.
    - Kiểm tra Redis: Thấy Key token đang tồn tại.
- **Bố cục:** Screenshot Postman kết quả API thành công lồng ghép với ảnh Terminal log của Spring Boot.

---

## PHẦN 3: DOCKER & MICROSERVICES

### Slide 7: Tư duy thiết kế Microservices
- **Nội dung:**
    - **Tại sao tách service?** Kể cả khi dùng chung 1 Database, việc tách service mang lại:
        - **Scaling:** Mở rộng độc lập phần xử lý (CPU/RAM) cho dịch vụ nặng.
        - **Deployment:** Triển khai từng tính năng mà không làm sập toàn bộ hệ thống.
        - **Isolation:** Cách ly lỗi (một module crash không kéo theo cả hệ thống).
        - **Teamwork:** Nhiều team làm việc song song, giảm xung đột code.
- **Bố cục:** Sơ đồ so sánh Monolith vs Microservices (màu sáng) với 4 icon đặc trưng cho 4 lợi ích trên.

### Slide 7.5: Containerization với Docker
- **Nội dung:**
    - **Vấn đề:** "Code chạy máy em nhưng không chạy trên server" (khác biệt OS, Library).
    - **Giải pháp:** Đóng gói ứng dụng + Runtime + Config vào một Container duy nhất.
    - **Lợi ích:** Đồng nhất môi trường (Dev/Test/Prod), khởi động nhanh, cô lập tài nguyên.
- **Bố cục:** Hình minh họa ứng dụng trong một chiếc "thùng Container" chứa đầy đủ Java, Libs, Config.

### Slide 8: Tối ưu hóa Docker (Dev & Multi-stage Build)
- **Nội dung:**
    - **Development:** Dev Container & Spring DevTools giúp nhất quán môi trường và hot-reload code.
    - **Multi-stage Build (Tối ưu Production):**
        - **Stage 1 (Build):** Dùng Maven + JDK để compile và đóng gói file JAR.
        - **Stage 2 (Runtime):** Chỉ dùng JRE siêu nhẹ để chạy JAR, loại bỏ toàn bộ source code và tool build.
    - **Kết quả:** Giảm kích thước từ ~800MB xuống ~250MB, tăng tính bảo mật.
- **Bố cục:** Sơ đồ 2 giai đoạn build Docker kèm bảng so sánh dung lượng (Before vs After).

### Slide 9: Điều phối với Docker Compose
- **Nội dung:**
    - **Chức năng:** Khởi chạy cụm dịch vụ (App + MySQL + Redis) chỉ với 1 lệnh.
    - **Networking:** Các container tự nhìn thấy nhau qua service name (ví dụ: `db:3306`).
    - **Persistence:** Sử dụng Volumes để dữ liệu MySQL không bị mất khi container restart.
- **Bố cục:** Sơ đồ kết nối mạng nội bộ giữa 3 container App - DB - Cache.

### Slide 10: [DEMO 2] Local Orchestration
- **Nội dung:**
    - Chạy lệnh `docker-compose up -d`.
    - Kiểm tra trạng thái bằng `docker ps`.
    - Xem log khởi động của hệ thống để xác nhận các kết nối DB/Redis đã READY.
- **Bố cục:** Ảnh chụp màn hình Terminal với danh sách các container đang ở trạng thái "Up".

---

## PHẦN 4: KUBERNETES ORCHESTRATION

### Slide 11: Kubernetes Orchestration
- **Nội dung:**
    - **Self-healing:** Tự động phát hiện và hồi phục sự cố.
    - **Scalability:** Sẵn sàng mở rộng quy mô tức thì.
    - **Service Discovery:** Tự động điều phối traffic qua lớp Service.
- **Bố cục:** Sơ đồ Master Node điều phối các Pod với tính năng tự chữa lành (Self-healing).

### Slide 12: External Configuration Management
- **Nội dung:**
    - **ConfigMap:** Lưu các config không nhạy cảm (URL, Port).
    - **Secrets:** Lưu Password, JWT Secret Key (mã hóa base64).
    - **Lợi ích:** Thay đổi cấu hình không cần build lại Docker image.
- **Bố cục:** Hình ảnh tách file `.env` vào 2 đối tượng ConfigMap và Secret trong K8s.

### Slide 13: K8s Infrastructure Services
- **Nội dung:**
    - MySQL và Redis được triển khai dưới dạng các Service riêng trong cluster.
    - Cơ chế **Internal DNS**: App kết nối tới MySQL qua host `mysql-service`.
    - Đảm bảo High Availability và dễ dàng mở rộng.
- **Bố cục:** Biểu đồ App Pod gọi Database qua lớp Service ClusterIP.

### Slide 14: [DEMO 3] K8s Operations
- **Nội dung:**
    - Chạy `kubectl apply -f k8s/`.
    - Kiểm tra Pods: `kubectl get pods`.
    - Truy cập ứng dụng qua URL của Minikube.
- **Bố cục:** Screenshot lệnh `kubectl get all` hiển thị đầy đủ các resource đang chạy.

---

## PHẦN 5: CI/CD & CHẤT LƯỢNG MÃ NGUỒN

### Slide 15: GitHub Actions Pipeline
- **Nội dung:**
    - **Trigger:** Tự động chạy khi có Push hoặc Pull Request vào nhánh chính.
    - **Workflow:** Checkout → Setup Java 21 → Maven Build → Run Tests → Docker Build & Push.
- **Bố cục:** Sơ đồ Pipeline dạng Flowchart các bước thực hiện tự động.

### Slide 16: Quality Gate & Testing
- **Nội dung:**
    - **JaCoCo:** Đo lường độ bao phủ (Coverage). Dự án đạt >80%.
    - **Unit Test & Integration Test:** Đảm bảo business logic chính xác.
    - **Quality Gate:** Pipeline sẽ FAIL nếu test lỗi hoặc coverage không đạt.
- **Bố cục:** Biểu đồ tròn (Pie chart) hiển thị tỷ lệ Coverage xanh/đỏ từ JaCoCo report.

### Slide 17: Tài liệu API & Hướng dẫn
- **Nội dung:**
    - **Swagger UI:** Tự động tạo document API, cho phép thử nghiệm trực tiếp trên trình duyệt.
    - **README & GUIDES:** Hướng dẫn cài đặt, vận hành hệ thống chi tiết cho dev mới.
- **Bố cục:** Ảnh chụp giao diện Swagger với các mã màu cho GET (xanh), POST (xanh lá), DELETE (đỏ).

### Slide 18: Tổng kết & Hướng phát triển
- **Nội dung:**
    - **Đã đạt được:** Microservices architecture, Bảo mật JWT/Redis, Full CI/CD & K8s.
    - **Kế hoạch:** Tích hợp Kafka (Message Queue), ELK Stack (Logging), Prometheus (Monitoring).
- **Bố cục:** Danh sách Checklist hoàn thành và biểu tượng "Roadmap" cho tương lai.
