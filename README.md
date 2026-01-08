# demo

## Dockerized Development (Java 21 + DevTools)

### Prerequisites
- Docker Desktop with Compose V2.
- Java 21 for any IDE tasks you run locally (container uses the Maven wrapper + JDK 21 image).
- Docker Compose will mount the Maven cache and the project root so IntelliJ saves can trigger DevTools restarts.

### Workflow
1. Start the stack with live reload:
```powershell
cd D:/demo
docker compose up --build
```
2. The `app` service runs `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` inside `maven:3.11.4-jdk-21-slim`. `Ctrl+S` in IntelliJ will update volume-mounted sources (`./:/workspace:delegated`) so Spring Boot DevTools sees the change and restarts automatically.
3. Access the app at `http://localhost:8080`; observe MySQL (port 3306) and Redis (port 6379) coming up through Compose.
4. When you are done, stop the containers to free ports:
```powershell
docker compose down
```

### IntelliJ Tips
- Enable **Build project automatically** under `Settings | Advanced Settings` so the IDE recompiles on save.
- In the Registry (`Ctrl+Shift+A` -> _Registry_), enable `compiler.automake.allow.when.app.running` so IntelliJ emits class files even while the app is running.
- Keep the Maven wrapper permissioned (`chmod +x mvnw` already part of the Compose command) and rely on the Docker volume for dependencies: the `m2_repo` volume caches downloads.
- If you need to run code inspection locally, point IntelliJ to `mvnw spring-boot:run` or a dedicated run configuration that starts the Dockerized app (IDE-run is optional; Compose is the primary dev server).

### Additional Notes
- Spring Boot DevTools is included with scope `runtime` and only kicks in during development; it is excluded from packaged artifacts.
- Environment variables such as `SPRING_PROFILES_ACTIVE=dev` and `SPRING_DEVTOOLS_RESTART_ENABLED=true` are set in Compose to keep the experience fast.
- Rebuild the container only when dependencies or Dockerfiles change: `docker compose up --build` once, then rely on live reload for code changes.
