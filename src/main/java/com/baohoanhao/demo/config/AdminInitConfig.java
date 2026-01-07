package com.baohoanhao.demo.config;

import com.baohoanhao.demo.entity.Role;
import com.baohoanhao.demo.entity.User;
import com.baohoanhao.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
@Slf4j
public class AdminInitConfig {

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.full-name:Admin}")
    private String adminFullName;

    @Value("${app.admin.phone:}")
    private String adminPhone;

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.info("Admin bootstrap skipped: missing app.admin.email or app.admin.password");
                return;
            }

            Optional<User> existing = userRepository.findByEmail(adminEmail);
            if (existing.isPresent()) {
                log.info("Admin user already exists: {}", adminEmail);
                return;
            }

            User admin = User.builder()
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .phone(adminPhone != null && !adminPhone.isBlank() ? adminPhone : null)
                    .passwordHash(encoder.encode(adminPassword))
                    .active(true)
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Admin user created: {}", adminEmail);
        };
    }
}
