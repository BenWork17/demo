# Hướng dẫn Vận hành Hệ thống (CI/CD & Kubernetes)

Dự án này đã được hiện đại hóa từ chạy local thông thường sang mô hình Cloud-Native (vận hành trên nền tảng đám mây).

## 0. Tại sao chúng ta làm việc này? (Tư duy DevOps)
*   **Mô phỏng Production:** Thay vì chạy `java -jar` đơn giản, chúng ta dùng K8s để giả lập môi trường thực tế của các công ty lớn.
*   **Tính sẵn sàng cao (Self-healing):** Nếu ứng dụng bị crash, K8s sẽ tự động khởi động lại nó mà không cần bạn can thiệp.
*   **Quản lý tập trung:** Mọi thứ từ App, DB, Redis đến cấu hình đều được định nghĩa bằng code (Infrastructure as Code), giúp dễ dàng tái tạo hệ thống trên bất kỳ máy chủ nào.

## 1. Thành phần hệ thống chi tiết

### A. Dockerization (Dockerfile)
*   **Mục đích:** Tạo ra một "thùng container" chứa mọi thứ app cần (JDK, Lib, Code).
*   **Giá trị:** Giải quyết triệt để lỗi "Code chạy máy em nhưng chết máy server". Một khi Image đã build xong, nó sẽ chạy giống hệt nhau ở mọi nơi.

### B. Kubernetes & Minikube (Thư mục /k8s)
K8s là "nhạc trưởng" điều phối các container. Minikube là "sân chơi" K8s ngay trên máy tính của bạn.
*   **config.yaml (ConfigMap/Secret):** Lưu biến môi trường. Tách biệt hoàn toàn Code và Data. Bạn có thể đổi Password DB mà không cần sửa 1 dòng code nào.
*   **infrastructure.yaml:** Chạy các dịch vụ nền (MySQL, Redis). K8s giúp App tự tìm thấy DB thông qua tên dịch vụ (Service Name) thay vì IP tĩnh hay `localhost`.
*   **deployment.yaml:** Bản thiết kế vận hành. Bạn muốn chạy bao nhiêu bản sao (replicas)? Dùng bao nhiêu CPU/RAM? Tất cả nằm ở đây.

### C. CI/CD (GitHub Actions)
*   **Mục đích:** "Người gác cổng" tự động.
*   **Cơ chế:** Ép buộc code phải đạt chuẩn chất lượng (Test coverage > 80%) và build thành công mới được coi là hoàn tất. Điều này đảm bảo hệ thống luôn ổn định mỗi khi có tính năng mới.

---

## 1.5. Kịch bản Demo sức mạnh của Kubernetes
Dưới đây là 3 tình huống thực tế giúp bạn giải thích cho người xem về lợi ích của K8s:

### Màn 1: Khả năng tự phục hồi (Self-healing)
*   **Ngữ cảnh:** Giả sử Server bị lỗi phần cứng hoặc ứng dụng bị crash đột ngột giữa đêm.
*   **Thực hiện:** `kubectl delete pod [tên-pod-backend]`
*   **Kết quả:** K8s phát hiện số lượng Pod thực tế (0) thấp hơn số lượng mong muốn (1), nó lập tức khởi tạo Pod mới trong 1-2 giây. Hệ thống vẫn online mà không cần nhân viên DevOps can thiệp thủ công.

### Màn 2: Khả năng mở rộng (Scaling)
*   **Ngữ cảnh:** Sắp tới ngày khuyến mãi (Black Friday) hoặc có chiến dịch marketing, lượng người truy cập tăng đột biến gấp 10 lần.
*   **Thực hiện:** `kubectl scale deployment backend-deployment --replicas=3`
*   **Kết quả:** Hệ thống lập tức nhân bản ứng dụng lên thành 3 thực thể để chia tải. Khi hết khuyến mãi, bạn chỉ cần scale về 1 để tiết kiệm tài nguyên.

### Màn 3: Giám sát trực quan (Dashboard)
*   **Ngữ cảnh:** Khi cần báo cáo cho quản lý về sức khỏe hệ thống hoặc theo dõi tài nguyên CPU/RAM một cách tổng thể.
*   **Thực hiện:** `minikube dashboard`
*   **Kết quả:** Một giao diện web hiện đại hiện ra, cung cấp cái nhìn toàn cảnh về hệ thống thay vì các dòng lệnh khô khan.

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
*   **theo dõi các pod:**kubectl get pods -w

minikube image load demo-app:v1
minikube service backend-service
