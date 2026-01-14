** | 3-4 phút | 
| **Optimized Multi-stage** | Stage 2: `21-jre-alpine` | JRE Alpine (40MB)<br/>JAR file (60MB) | **~150MB** ⭐ | 3-4 phút |

**Giảm được: 70-80% kích thước!**

---

**Lợi ích của Multi-stage Build:**

#### 1. 🎯 Giảm kích thước Image
- **Tại sao quan trọng?**
  - Pull image nhanh hơn (quan trọng cho CI/CD)
  - Tốn ít băng thông
  - Ít disk space trên server
  - Kubernetes pull image nhanh hơn → startup time ngắn hơn

#### 2. 🔒 Bảo mật mã nguồn
- Source code không nằm trong final image
- Maven dependencies build-time không ship trong production
- Chỉ có JAR file đã compiled
- **Principle:** Separation of build artifacts và runtime artifacts

#### 3. ⚡ Tối ưu Layer Caching
```dockerfile
# Layer 1: Base image (cached, rarely changes)
FROM eclipse-temurin:21-jre-alpine

# Layer 2: Application jar (changes frequently)
COPY --from=build /app/target/*.jar app.jar
```
- Docker cache layers
- Chỉ rebuild layers thay đổi
- **Ví dụ:** Thay đổi code → Chỉ rebuild layer cuối

#### 4. 🏃 Faster Deployment
- Smaller image = faster transfer
- Kubernetes rollout nhanh hơn
- Less time in "ContainerCreating" state

---

**So sánh Build Process:**

**Traditional Single-stage:**
```
Build Image ──────▶ Push to Registry ──────▶ Deploy
   (800MB)              (5 minutes)           (slow)
```

**Multi-stage Build:**
```
Build Stage ──────▶ Copy JAR ──────▶ Final Image ──────▶ Push ──────▶ Deploy
  (800MB)            (Only JAR)       (150MB)            (1 min)       (fast)
  (Discarded)
```

---

**Best Practices:**

#### 1. Use Alpine base images
```dockerfile
# ❌ Heavy
FROM eclipse-temurin:21-jre  # ~200MB

# ✅ Lightweight  
FROM eclipse-temurin:21-jre-alpine  # ~50MB
```

#### 2. Leverage Docker layer caching
```dockerfile
# Copy dependencies first (changes less frequently)
COPY pom.xml .
RUN ./mvnw dependency:go-offline

# Copy source later (changes more frequently)
COPY src ./src
RUN ./mvnw package
```

#### 3. Use .dockerignore
```
# .dockerignore
target/
.git/
.idea/
*.log
README.md
```

#### 4. Minimize layers
```dockerfile
# ❌ Multiple layers
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y vim

# ✅ Single layer
RUN apt-get update && \
    apt-get install -y curl vim && \
    rm -rf /var/lib/apt/lists/*
```

---

**Build Commands:**

```bash
# Build multi-stage image
docker build -t demo-app:v1 .

# Check image size
docker images demo-app:v1

# Inspect layers
docker history demo-app:v1

# Run container
docker run -p 8080:8080 demo-app:v1
```

**Output:**
```
REPOSITORY   TAG    SIZE      CREATED
demo-app     v1     150MB     10 seconds ago
```

---

**Advanced: Target specific build stage**

```bash
# Build only the "build" stage (for debugging)
docker build --target build -t demo-app:build .

# Build only the final stage (default)
docker build -t demo-app:runtime .
```

**Ghi chú thiết kế:**
- Code blocks với syntax highlighting
- Bảng so sánh với màu xanh (optimized) và đỏ (unoptimized)
- Icons Docker whale
- Biểu đồ cột so sánh image sizes

---

### SLIDE 9: Điều phối với Docker Compose

**Tiêu đề:**
## 🎼 Điều Phối với Docker Compose

**Mô tả:** Docker Compose = Orchestration tool cho multi-container applications

---

**Sơ đồ kết nối giữa các Service:**

```
┌─────────────────────────────────────────────────────────────┐
│               DOCKER COMPOSE ARCHITECTURE                    │
└─────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │   HOST MACHINE   │
                    │  localhost:8080  │
                    └────────┬─────────┘
                             │ Port Mapping
                    ┌────────▼─────────┐
                    │   Docker Bridge  │
                    │    Network       │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
  ┌─────▼─────┐       ┌─────▼─────┐       ┌─────▼─────┐
  │   App     │       │   MySQL   │       │   Redis   │
  │ Container │◀─────▶│ Container │       │ Container │
  │  :8080    │       │  :3306    │       │  :6379    │
  └───────────┘       └───────────┘       └───────────┘
       │                    │                    │
       │                    │                    │
  ┌────▼────┐          ┌───▼────┐          ┌───▼────┐
  │ Volume  │          │ Volume │          │ Volume │
  │ m2_repo │          │mysql_  │          │redis_  │
  │         │          │data    │          │data    │
  └─────────┘          └────────┘          └────────┘
```

---

**docker-compose.yml Configuration:**

```yaml
version: "3.8"

services:
  # =====================================
  # MySQL Database Service
  # =====================================
  mysql:
    image: mysql:8.0.44-debian
    container_name: mysql-8
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}  # From .env
      MYSQL_DATABASE: ${MYSQL_DATABASE}
    ports:
      - "3306:3306"  # Host:Container
    volumes:
      - mysql_data:/var/lib/mysql  # Persistent storage
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  # =====================================
  # Redis Cache Service
  # =====================================
  redis:
    image: redis:7.4-alpine
    container_name: redis-7
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes  # Enable persistence
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  # =====================================
  # Spring Boot Application Service
  # =====================================
  app:
    image: maven:3.9.9-eclipse-temurin-21
    container_name: demo-app
    working_dir: /workspace
    command: >
      bash -lc "
        chmod +x mvnw && 
        ./mvnw spring-boot:run 
          -Dspring-boot.run.profiles=${SPRING_PROFILES_ACTIVE}
          -DskipTests
      "
    ports:
      - "8080:8080"
    environment:
      # Spring Boot configurations
      SPRING_OUTPUT_ANSI_ENABLED: ALWAYS
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      SPRING_DEVTOOLS_RESTART_ENABLED: "true"
      
      # Database connection
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      
      # Redis connection
      SPRING_DATA_REDIS_HOST: redis  # Service name as hostname!
      SPRING_DATA_REDIS_PORT: 6379
      
      # OAuth
      APP_OAUTH_ENABLED: ${APP_OAUTH_ENABLED}
    volumes:
      - ./:/workspace:delegated  # Source code mount
      - m2_repo:/root/.m2        # Maven cache
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - app-network

# =====================================
# Networks
# =====================================
networks:
  app-network:
    driver: bridge

# =====================================
# Volumes (Persistent Storage)
# =====================================
volumes:
  mysql_data:      # MySQL database files
  redis_data:      # Redis RDB/AOF files
  m2_repo:         # Maven dependencies cache
```

---

**4 Tính năng chính:**

#### 1. 🔗 Internal Networking

**Cơ chế:**
- Docker Compose tạo private network `app-network`
- Mỗi service có hostname = service name
- Services có thể gọi nhau qua tên, không cần IP

**Ví dụ:**
```yaml
# Application connects to MySQL
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/demo_db
                                    #  ↑
                                    # Service name, NOT localhost!
```

**DNS Resolution:**
```
app container → Resolve "mysql" → Docker internal DNS → 172.18.0.2 (MySQL IP)
```

**Lợi ích:**
- Không cần hardcode IP addresses
- IP có thể thay đổi, hostname không đổi
- Dễ dàng di chuyển giữa các môi trường

---

#### 2. 💾 Volume Persistence

**3 loại volumes:**

**a) Named Volumes** (Managed by Docker)
```yaml
volumes:
  mysql_data:/var/lib/mysql
  
# Data persists even after container removal
# Location: /var/lib/docker/volumes/mysql_data/_data
```

**b) Bind Mounts** (Host filesystem)
```yaml
volumes:
  ./:/workspace:delegated
  
# Real-time code sync
# Changes on host → immediately visible in container
```

**c) Anonymous Volumes**
```yaml
volumes:
  - /app/node_modules  # Not mapped to host
```

**Volume Commands:**
```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect mysql_data

# Backup volume
docker run --rm -v mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz /data

# Restore volume
docker run --rm -v mysql_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql_backup.tar.gz -C /
```

**Tại sao quan trọng?**
- Database không bị mất khi restart container
- Development: Maven dependencies được cache
- Production: Logs, uploads được lưu trữ

---

#### 3. 🔄 Cơ chế Auto Restart

**Restart Policies:**

```yaml
restart: "no"              # Never restart (default)
restart: always            # Always restart
restart: on-failure        # Only on error exit codes
restart: unless-stopped    # Restart except when manually stopped
```

**Trong dự án:**
```yaml
app:
  restart: unless-stopped  # Best for development
  
# Behaviors:
# - Container crashes → Auto restart
# - Docker daemon restarts → Auto restart
# - Manual "docker stop" → Don't restart
```

**Use cases:**
- **Development:** `unless-stopped` - restart khi code lỗi
- **Production:** `always` - maximize uptime
- **Testing:** `no` - kiểm soát manual

**Exit codes:**
```
Exit 0   → Normal exit, no restart
Exit 1   → General error, restart if policy allows
Exit 137 → SIGKILL (OOM), restart
Exit 143 → SIGTERM (graceful shutdown), respect policy
```

---

#### 4. ⚙️ Environment Configuration với .env

**.env file:**
```bash
# Database Configuration
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=demo_db
MYSQL_USER=root
MYSQL_PASSWORD=root

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
APP_OAUTH_ENABLED=true

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
```

**docker-compose.yml sử dụng:**
```yaml
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}  # Reads from .env
  MYSQL_DATABASE: ${MYSQL_DATABASE}
```

**Lợi ích:**
- ✅ Không commit sensitive data vào Git
- ✅ Mỗi developer có config riêng
- ✅ Dễ dàng switch giữa môi trường (dev/staging/prod)
- ✅ `.env` trong `.gitignore`, commit `.env.example`

**.env.example** (Template for new developers):
```bash
# Copy this to .env and fill in your values

MYSQL_ROOT_PASSWORD=your_password_here
MYSQL_DATABASE=demo_db
# ... other configs
```

---

**Health Checks:**

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s    # Check every 10 seconds
  timeout: 5s      # Wait 5 seconds for response
  retries: 5       # Try 5 times before marking unhealthy
  start_period: 30s # Grace period for startup
```

**States:**
- `starting` → Container just started
- `healthy` → Health check passing
- `unhealthy` → Health check failing

**depends_on với conditions:**
```yaml
app:
  depends_on:
    mysql:
      condition: service_healthy  # Wait for MySQL to be healthy
    redis:
      condition: service_healthy
```

**Tại sao cần?**
- App cần MySQL sẵn sàng trước khi connect
- Tránh connection errors lúc startup

---

**Docker Compose Commands:**

```bash
# Start all services
docker compose up

# Start in background (detached mode)
docker compose up -d

# Rebuild images before starting
docker compose up --build

# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v

# View logs
docker compose logs
docker compose logs -f app  # Follow app logs only

# List running services
docker compose ps

# Execute command in service
docker compose exec app bash
docker compose exec mysql mysql -u root -proot

# Restart specific service
docker compose restart app

# Scale services
docker compose up -d --scale app=3  # Run 3 instances of app
```

---

**Troubleshooting:**

**Issue 1: Port already in use**
```bash
Error: Bind for 0.0.0.0:3306 failed: port is already allocated

# Solution: Check what's using the port
netstat -ano | findstr :3306  # Windows
lsof -i :3306                 # Linux/Mac

# Kill the process or change port mapping
ports:
  - "3307:3306"  # Use host port 3307 instead
```

**Issue 2: Container exiting immediately**
```bash
# Check logs
docker compose logs mysql

# Common causes:
# - Wrong environment variables
# - Volume permission issues
# - Port conflicts
```

**Issue 3: Services can't communicate**
```bash
# Ensure same network
docker compose exec app ping mysql

# Check network
docker network inspect demo_app-network
```

**Ghi chú thiết kế:**
- Sơ đồ architecture lớn với 3 containers
- Code blocks YAML với syntax highlighting
- Tables so sánh restart policies
- Icons: 🔗 (network), 💾 (storage), 🔄 (restart), ⚙️ (config)

---

### SLIDE 10: [DEMO 2] Local Orchestration

**Tiêu đề:**
## 🎯 [DEMO 2] Local Orchestration với Docker Compose

**Mục đích:** Khởi động và quản lý toàn bộ stack (App + MySQL + Redis)

---

#### BƯỚC 1: Khởi động toàn bộ hệ thống

**Command:**
```bash
cd D:/demo
docker compose up --build
```

**Output (Expected):**
```
[+] Running 4/4
 ✔ Network demo_app-network    Created
 ✔ Container mysql-8           Started
 ✔ Container redis-7           Started
 ✔ Container demo-app          Started

Attaching to mysql-8, redis-7, demo-app

mysql-8   | 2026-01-13 10:00:00 [Note] mysqld: ready for connections
redis-7   | Ready to accept connections
demo-app  | 
demo-app  |   .   ____          _            __ _ _
demo-app  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
demo-app  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
demo-app  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
demo-app  |   '  |____| .__|_| |_|_| |_\__, | / / / /
demo-app  |  =========|_|==============|___/=/_/_/_/
demo-app  | 
demo-app  |  :: Spring Boot ::                (v4.0.1)
demo-app  | 
demo-app  | 2026-01-13 10:00:10 INFO  c.b.demo.DemoApplication - Starting DemoApplication
demo-app  | 2026-01-13 10:00:12 INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http)
demo-app  | 2026-01-13 10:00:12 INFO  c.b.demo.DemoApplication - Started DemoApplication in 5.234 seconds
```

**Timeline:**
```
0s   → Start MySQL container
5s   → MySQL health check passing
6s   → Start Redis container
8s   → Redis health check passing
9s   → Start App container (waits for MySQL & Redis healthy)
15s  → App fully started and ready
```

**Kiểm tra:** Browser mở `http://localhost:8080` → Spring Boot default page

---

#### BƯỚC 2: Kiểm tra trạng thái containers

**Command:**
```bash
docker compose ps
```

**Output:**
```
NAME       IMAGE                           COMMAND                  SERVICE   CREATED          STATUS                    PORTS
demo-app   maven:3.9.9-eclipse-temurin-21  "bash -lc 'chmod +x …"   app       30 seconds ago   Up 20 seconds (healthy)   0.0.0.0:8080->8080/tcp
mysql-8    mysql:8.0.44-debian             "docker-entrypoint.s…"   mysql     30 seconds ago   Up 25 seconds (healthy)   0.0.0.0:3306->3306/tcp, 33060/tcp
redis-7    redis:7.4-alpine                "docker-entrypoint.s…"   redis     30 seconds ago   Up 25 seconds (healthy)   0.0.0.0:6379->6379/tcp
```

**Key indicators:**
- ✅ STATUS: `Up` - Container running
- ✅ `(healthy)` - Health checks passing
- ✅ PORTS: `0.0.0.0:8080->8080/tcp` - Port mappings active

**Alternative check:**
```bash
# Docker native command
docker ps

# Get only container names
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

---

#### BƯỚC 3: Health Check từng service

**3.1 MySQL Health Check**

**Command:**
```bash
# Check từ bên ngoài
docker exec mysql-8 mysqladmin ping -h localhost -u root -proot

# Output: mysqld is alive
```

**Detailed check:**
```bash
docker exec -it mysql-8 mysql -u root -proot

mysql> SELECT VERSION();
+-------------------------+
| VERSION()               |
+-------------------------+
| 8.0.44                  |
+-------------------------+

mysql> SHOW DATABASES;
+--------------------+
| Database           |
+--------------------+
| demo_db            |  ← Our database
| information_schema |
| mysql              |
| performance_schema |
| sys                |
+--------------------+

mysql> USE demo_db;
mysql> SHOW TABLES;
+---------------------+
| Tables_in_demo_db   |
+---------------------+
| flyway_schema_history|  ← Flyway migrations
| users               |  ← Our tables
+---------------------+

mysql> SELECT COUNT(*) FROM users;
+----------+
| COUNT(*) |
+----------+
|        1 |  ← Admin user created
+----------+

mysql> EXIT;
```

**Check logs:**
```bash
docker compose logs mysql | tail -20

# Should see: "mysqld: ready for connections"
```

---

**3.2 Redis Health Check**

**Command:**
```bash
# Ping Redis
docker exec redis-7 redis-cli ping

# Output: PONG
```

**Detailed check:**
```bash
docker exec -it redis-7 redis-cli

127.0.0.1:6379> INFO server
# Server
redis_version:7.4.0
redis_mode:standalone
os:Linux
arch_bits:64
tcp_port:6379

127.0.0.1:6379> INFO persistence
# Persistence
loading:0
rdb_changes_since_last_save:0
aof_enabled:1  ← AOF persistence enabled

127.0.0.1:6379> DBSIZE
(integer) 0  ← No keys yet (no users logged in)

127.0.0.1:6379> CONFIG GET maxmemory
1) "maxmemory"
2) "0"  ← No memory limit (uses all available)

127.0.0.1:6379> EXIT
```

**Check logs:**
```bash
docker compose logs redis | tail -10

# Should see: "Ready to accept connections"
```

---

**3.3 Application Health Check**

**Command:**
```bash
# Check if Spring Boot is responding
curl http://localhost:8080/actuator/health

# Output:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.4.0"
      }
    }
  }
}
```

**Test API endpoint:**
```bash
# Register new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "fullName": "Test User"
  }'

# Expected: 201 Created
{
  "status": "success",
  "message": "User registered successfully"
}
```

**Check application logs:**
```bash
docker compose logs -f app

# Should see:
# - "Started DemoApplication"
# - "Tomcat started on port(s): 8080"
# - "Flyway migrations completed"
# - No ERROR or Exception messages
```

---

#### BƯỚC 4: Kiểm tra connectivity giữa services

**4.1 App → MySQL**
```bash
docker compose exec app bash

# Inside app container
apt-get update && apt-get install -y mysql-client
mysql -h mysql -u root -proot -D demo_db -e "SELECT 1"

# Output: 1
# ✅ App can connect to MySQL
```

**4.2 App → Redis**
```bash
docker compose exec app bash

# Inside app container
apt-get update && apt-get install -y redis-tools
redis-cli -h redis ping

# Output: PONG
# ✅ App can connect to Redis
```

**4.3 Network inspection**
```bash
docker network inspect demo_app-network

# Check "Containers" section
"Containers": {
    "abc123": {
        "Name": "demo-app",
        "IPv4Address": "172.18.0.4/16"
    },
    "def456": {
        "Name": "mysql-8",
        "IPv4Address": "172.18.0.2/16"
    },
    "ghi789": {
        "Name": "redis-7",
        "IPv4Address": "172.18.0.3/16"
    }
}
```

---

#### BƯỚC 5: Monitoring & Logs

**Real-time logs (all services):**
```bash
docker compose logs -f
```

**Filter by service:**
```bash
docker compose logs -f app
docker compose logs -f mysql
docker compose logs -f redis
```

**Last 50 lines:**
```bash
docker compose logs --tail=50
```

**Search in logs:**
```bash
docker compose logs | grep "ERROR"
docker compose logs app | grep "Started DemoApplication"
```

**Resource usage:**
```bash
docker stats

# Output:
CONTAINER    CPU %   MEM USAGE / LIMIT     MEM %   NET I/O         BLOCK I/O
demo-app     5.2%    512MiB / 2GiB        25.6%   1.5MB / 850kB   10MB / 5MB
mysql-8      2.1%    256MiB / 2GiB        12.8%   500kB / 1.2MB   50MB / 20MB
redis-7      0.3%    10MiB / 2GiB          0.5%   100kB / 50kB    1MB / 500kB
```

---

#### BƯỚC 6: Dừng và dọn dẹp

**Stop services (keep volumes):**
```bash
docker compose down

# Containers stopped and removed
# Networks removed
# Volumes KEPT (data persists)
```

**Stop và xóa volumes:**
```bash
docker compose down -v

# ⚠️ WARNING: This deletes MySQL database and Redis data!
```

**Restart individual service:**
```bash
docker compose restart app
```

**Rebuild specific service:**
```bash
docker compose up -d --build app
```

---

**KẾT LUẬN DEMO 2:**

✅ **Thành công nếu:**
- Cả 3 containers đều `Up` và `healthy`
- App accessible tại `http://localhost:8080`
- MySQL có database `demo_db` với tables
- Redis responding to PING
- Services có thể communicate với nhau
- Logs không có ERROR messages

✅ **Đã demonstrate:**
- Docker Compose multi-container orchestration
- Service discovery via DNS
- Health checks working
- Volume persistence
- Environment configuration
- Logging và monitoring

**Ghi chú thiết kế:**
- Terminal screenshots với output màu xanh lá (success)
- Step-by-step layout với số thứ tự rõ ràng
- Checkmarks (✅) cho mỗi verification point
- Code blocks với syntax highlighting
- Highlight commands vs output

---

## PHẦN 4: KUBERNETES ORCHESTRATION

---

### SLIDE 11: Triển khai trên Kubernetes (K8s)

**Tiêu đề:**
## ☸️ Triển Khai trên Kubernetes

**Subtitle:** Container Orchestration cho Production

---

**Kubernetes Architecture Diagram:**

```
┌────────────────────────────────────────────────────────────────┐
│                    KUBERNETES CLUSTER                          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                      Master Node                          │ │
│  │  ┌──────────┐  ┌───────────┐  ┌──────────┐             │ │
│  │  │ API      │  │ Scheduler │  │Controller│             │ │
│  │  │ Server   │  │           │  │ Manager  │             │ │
│  │  └──────────┘  └───────────┘  └──────────┘             │ │
│  └──────────────────────────────────────────────────────────┘ │
│                           │                                    │
│  ┌────────────────────────┴────────────────────────┐          │
│  │                  Worker Node (Minikube)         │          │
│  │                                                  │          │
│  │  ┌─────────────────────────────────────────┐   │          │
│  │  │          Pod: backend-pod-abc123        │   │          │
│  │  │  ┌────────────────────────────────┐    │   │          │
│  │  │  │  Container: backend-app        │    │   │          │
│  │  │  │  Image: demo-app:v1            │    │   │          │
│  │  │  │  Port: 8080                    │    │   │          │
│  │  │  └────────────────────────────────┘    │   │          │
│  │  └─────────────────────────────────────────┘   │          │
│  │                                                  │          │
│  │  ┌─────────────────────────────────────────┐   │          │
│  │  │          Pod: mysql-infra               │   │          │
│  │  │  ┌────────────────────────────────┐    │   │          │
│  │  │  │  Container: mysql              │    │   │          │
│  │  │  │  Image: mysql:8.0              │    │   │          │
│  │  │  └────────────────────────────────┘    │   │          │
│  │  └─────────────────────────────────────────┘   │          │
│  │                                                  │          │
│  │  ┌─────────────────────────────────────────┐   │          │
│  │  │          Pod: redis-infra               │   │          │
│  │  │  ┌────────────────────────────────┐    │   │          │
│  │  │  │  Container: redis              │    │   │          │
│  │  │  │  Image: redis:alpine