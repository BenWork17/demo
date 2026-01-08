package com.baohoanhao.demo.service;

import com.baohoanhao.demo.config.JwtProperties;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.entity.Role;
import com.baohoanhao.demo.entity.User;
import com.baohoanhao.demo.exception.BadRequestException;
import com.baohoanhao.demo.exception.UnauthorizedException;
import com.baohoanhao.demo.repository.UserRepository;
import com.baohoanhao.demo.security.JwtService;
import com.baohoanhao.demo.security.TokenStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 login service: trao đổi code lấy token, lấy profile, gắn role và phát hành JWT nội bộ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("oauth")
@ConditionalOnProperty(value = "app.oauth.enabled", havingValue = "true")
public class Oauth2LoginService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final JwtProperties jwtProperties;
    private final RestTemplate restTemplate;

    public AuthResponse handleCallback(String providerId, String code, String redirectUri) {
        ClientRegistration registration = findRegistration(providerId);
        Map<String, Object> tokenResponse = exchangeCodeForToken(registration, code, redirectUri);
        OAuthProfile profile = fetchProfile(providerId, registration, tokenResponse);
        User user = upsertUser(profile);
        return issueTokens(user);
    }

    private ClientRegistration findRegistration(String providerId) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(providerId);
        if (registration == null) {
            throw new BadRequestException("Provider không hợp lệ");
        }
        return registration;
    }

    private Map<String, Object> exchangeCodeForToken(ClientRegistration registration, String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        body.add(OAuth2ParameterNames.CODE, code);
        body.add(OAuth2ParameterNames.REDIRECT_URI, redirectUri);
        body.add(OAuth2ParameterNames.CLIENT_ID, registration.getClientId());
        body.add(OAuth2ParameterNames.CLIENT_SECRET, registration.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                registration.getProviderDetails().getTokenUri(),
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response == null || (!response.containsKey("access_token") && !response.containsKey("id_token"))) {
            throw new UnauthorizedException("Không nhận được token từ nhà cung cấp");
        }
        return response;
    }

    private OAuthProfile fetchProfile(String providerId, ClientRegistration registration, Map<String, Object> tokenResponse) {
        if ("google".equalsIgnoreCase(providerId)) {
            return fetchGoogleProfile(registration, tokenResponse);
        }
        if ("facebook".equalsIgnoreCase(providerId)) {
            return fetchFacebookProfile(registration, tokenResponse);
        }
        throw new BadRequestException("Provider không được hỗ trợ");
    }

    private OAuthProfile fetchGoogleProfile(ClientRegistration registration, Map<String, Object> tokenResponse) {
        String idToken = (String) tokenResponse.get("id_token");
        if (idToken == null) {
            throw new UnauthorizedException("Google không trả về id_token");
        }

        JwtDecoder decoder = buildGoogleDecoder(registration.getClientId());
        Jwt jwt = decoder.decode(idToken);

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String picture = jwt.getClaimAsString("picture");

        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Google không cung cấp email");
        }

        return new OAuthProfile("google", sub, email, name, picture);
    }

    private OAuthProfile fetchFacebookProfile(ClientRegistration registration, Map<String, Object> tokenResponse) {
        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new UnauthorizedException("Facebook không trả về access_token");
        }

        String userInfoUri = registration.getProviderDetails().getUserInfoEndpoint().getUri();
        String uri = userInfoUri + (userInfoUri.contains("?") ? "&" : "?") + "access_token=" + accessToken;

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = restTemplate.getForObject(uri, Map.class);
        if (profile == null || profile.get("id") == null) {
            throw new UnauthorizedException("Không lấy được thông tin người dùng Facebook");
        }

        String id = String.valueOf(profile.get("id"));
        String name = profile.get("name") != null ? profile.get("name").toString() : null;
        String email = profile.get("email") != null ? profile.get("email").toString() : null;
        String picture = extractFacebookPicture(profile);

        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Facebook chưa cung cấp email (cần cấp quyền email)");
        }

        return new OAuthProfile("facebook", id, email, name, picture);
    }

    private String extractFacebookPicture(Map<String, Object> profile) {
        Object pictureObj = profile.get("picture");
        if (!(pictureObj instanceof Map<?, ?> pictureMap)) {
            return null;
        }
        Object dataObj = pictureMap.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object urlObj = dataMap.get("url");
        return urlObj != null ? urlObj.toString() : null;
    }

    private User upsertUser(OAuthProfile profile) {
        Optional<User> existing = profile.email() != null ? userRepository.findByEmail(profile.email()) : Optional.empty();

        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isActive()) {
                throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
            }
            // Cập nhật tên nếu trống
            if (user.getFullName() == null && profile.name() != null) {
                user.setFullName(profile.name());
                userRepository.save(user);
            }
            return user;
        }

        if (profile.email() == null) {
            throw new BadRequestException("Không thể tạo tài khoản nếu thiếu email");
        }

        User user = User.builder()
                .email(profile.email())
                .fullName(profile.name() != null ? profile.name() : profile.email())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .active(true)
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        tokenStorageService.storeRefreshToken(
                user.getId().toString(),
                refreshToken,
                jwtProperties.getRefreshTokenExpiration()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    private JwtDecoder buildGoogleDecoder(String clientId) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation("https://accounts.google.com");
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && audiences.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience mismatch", null));
        };

        DelegatingOAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                audienceValidator
        );
        decoder.setJwtValidator(validators);
        return decoder;
    }

    private record OAuthProfile(String provider, String providerUserId, String email, String name, String picture) {
    }
}
