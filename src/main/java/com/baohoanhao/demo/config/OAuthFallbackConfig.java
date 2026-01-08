package com.baohoanhao.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Cung cấp ClientRegistrationRepository rỗng khi chưa cấu hình OAuth2.
 * Khi bật profile "oauth" với đầy đủ client-id/secret, Spring Boot sẽ tự tạo
 * InMemoryClientRegistrationRepository và bean này sẽ bị bỏ qua.
 */
@Configuration
@Profile("!oauth")
public class OAuthFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository noopClientRegistrationRepository() {
        // Trả về repository rỗng - không có provider nào được đăng ký
        return registrationId -> null;
    }
}
