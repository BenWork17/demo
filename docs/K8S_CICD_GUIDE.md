# Hướng dẫn Vận hành Hệ thống (CI/CD & Kubernetes)

Dự án này đã được hiện đại hóa từ chạy local thông thường sang mô hình Cloud-Native. Dưới đây là tóm tắt những gì chúng ta đã làm:

## 1. Thành phần hệ thống

### A. Dockerization (Dockerfile)
*   **Mục đích:** Đóng gói ứng dụng Java thành một "Container Image". Giúp ứng dụng chạy giống hệt nhau trên mọi máy tính (Local, Test, Production).
*   **Cơ chế:** Sử dụng Multi-stage build (Stage 1 để Build JAR, Stage 2 để chạy) giúp image nhẹ và bảo mật hơn.

### B. Kubernetes (Thư mục /k8s)
Thay vì chạy bằng Docker Compose, chúng ta dùng K8s để quản lý App:
*   **config.yaml (ConfigMap/Secret):** Tách cấu hình (DB URL, Passwords, JWT Key) ra khỏi code. Giúp thay đổi cấu hình mà không cần build lại code.
*   **infrastructure.yaml:** Chạy Database (MySQL) và Redis ngay bên trong cụm K8s.
*   **deployment.yaml:** Định nghĩa cách chạy App (số lượng bản sao, image sử dụng, cổng truy cập).

### C. CI/CD (GitHub Actions)
*   **Mục đích:** Kiểm soát chất lượng code tự động.
*   **Cơ chế:** Mỗi khi `git push`, GitHub sẽ tự chạy: Build -> Test -> Quét JaCoCo (phải >80% code được test) -> Build Docker.

---

## 2. Quy trình làm việc hàng ngày (Workflow)

Khi bạn code tính năng mới (ví dụ: Sửa/Xóa User):

### Bước 1: Kiểm tra chất lượng trên GitHub
1.  `git add .` -> `git commit -m "feat: add delete user"` -> `git push`.
2.  Lên GitHub xem tab **Actions**. Nếu hiện ✅ là code của bạn sạch và đủ Test.

### Bước 2: Triển khai lên K8s Local (Minikube)
1.  **Build Image mới:**
    `docker build -t demo-app:v2 .`
2.  **Đưa image vào Minikube:**
    `minikube image load demo-app:v2`
3.  **Cập nhật K8s:**
    Sửa `image: demo-app:v2` trong file `k8s/deployment.yaml`.
    Chạy: `kubectl apply -f k8s/deployment.yaml`
4.  **Kiểm tra Pod:**
    `kubectl get pods` (Đợi hiện Running 1/1).

### Bước 3: Truy cập App
`minikube service backend-service`

---

## 3. Các lệnh "Cứu hộ" (Troubleshooting)
*   **Xem lỗi code:** `kubectl logs -f deployment/backend-deployment`
*   **Xem lỗi K8s (tại sao pod ko chạy):** `kubectl describe pod [tên-pod]`
*   **Vào bên trong DB để xem dữ liệu:** `kubectl exec -it mysql-infra -- mysql -u root -proot -D demo_db`
