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

## 5. Tích hợp trong CI/CD
Trong quy trình CI/CD ([ci.yml](file:///d:/demo/.github/workflows/ci.yml)), khi lệnh `./mvnw clean verify` chạy:
- Các định nghĩa OpenAPI sẽ được kiểm tra tính hợp lệ.
- Tài liệu API luôn được cập nhật tương ứng với phiên bản code mới nhất.

## 6. Lưu ý cho K8S
Khi deploy lên Kubernetes, hãy đảm bảo cấu hình `Service` và `Ingress` cho phép truy cập port `8080` để xem Swagger UI từ bên ngoài nếu cần thiết.
