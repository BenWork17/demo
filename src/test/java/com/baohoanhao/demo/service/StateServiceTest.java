package com.baohoanhao.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StateService Tests")
class StateServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private StateService stateService;

    @Nested
    @DisplayName("storeState()")
    class StoreStateTests {

        @Test
        @DisplayName("should store state with redirect URL successfully")
        void storeState_ValidInput_Success() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "random-state-123";
            String redirectUrl = "https://example.com/callback";

            // Act
            stateService.storeState(state, redirectUrl);

            // Assert
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

            verify(valueOperations).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                ttlCaptor.capture(),
                unitCaptor.capture()
            );

            assertThat(keyCaptor.getValue()).isEqualTo("oauth_state:random-state-123");
            assertThat(valueCaptor.getValue()).isEqualTo(redirectUrl);
            assertThat(ttlCaptor.getValue()).isEqualTo(5L);
            assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("should store state with correct prefix")
        void storeState_CheckPrefix_UsesCorrectPrefix() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "abc123";
            String redirectUrl = "https://example.com";

            // Act
            stateService.storeState(state, redirectUrl);

            // Assert
            verify(valueOperations).set(
                eq("oauth_state:abc123"),
                eq(redirectUrl),
                eq(5L),
                eq(TimeUnit.MINUTES)
            );
        }
    }

    @Nested
    @DisplayName("validateAndGetRedirectUrl()")
    class ValidateAndGetRedirectUrlTests {

        @Test
        @DisplayName("should return redirect URL when state is valid")
        void validateAndGetRedirectUrl_ValidState_ReturnsRedirectUrl() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "valid-state";
            String redirectUrl = "https://example.com/callback";

            when(valueOperations.get("oauth_state:valid-state")).thenReturn(redirectUrl);

            // Act
            String result = stateService.validateAndGetRedirectUrl(state);

            // Assert
            assertThat(result).isEqualTo(redirectUrl);
            verify(redisTemplate).delete("oauth_state:valid-state");
        }

        @Test
        @DisplayName("should return null when state is invalid")
        void validateAndGetRedirectUrl_InvalidState_ReturnsNull() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "invalid-state";

            when(valueOperations.get("oauth_state:invalid-state")).thenReturn(null);

            // Act
            String result = stateService.validateAndGetRedirectUrl(state);

            // Assert
            assertThat(result).isNull();
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("should return null when state is null")
        void validateAndGetRedirectUrl_NullState_ReturnsNull() {
            // Act
            String result = stateService.validateAndGetRedirectUrl(null);

            // Assert
            assertThat(result).isNull();
            verify(valueOperations, never()).get(anyString());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("should return null when state is blank")
        void validateAndGetRedirectUrl_BlankState_ReturnsNull() {
            // Act
            String result = stateService.validateAndGetRedirectUrl("   ");

            // Assert
            assertThat(result).isNull();
            verify(valueOperations, never()).get(anyString());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("should delete state after successful validation")
        void validateAndGetRedirectUrl_ValidState_DeletesAfterValidation() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "test-state";
            when(valueOperations.get("oauth_state:test-state")).thenReturn("https://example.com");

            // Act
            stateService.validateAndGetRedirectUrl(state);

            // Assert
            verify(redisTemplate).delete("oauth_state:test-state");
        }

        @Test
        @DisplayName("should handle state used only once")
        void validateAndGetRedirectUrl_StateUsedTwice_ReturnsNullSecondTime() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String state = "one-time-state";
            when(valueOperations.get("oauth_state:one-time-state"))
                .thenReturn("https://example.com")
                .thenReturn(null);

            // Act - First use
            String firstResult = stateService.validateAndGetRedirectUrl(state);
            // Act - Second use
            String secondResult = stateService.validateAndGetRedirectUrl(state);

            // Assert
            assertThat(firstResult).isEqualTo("https://example.com");
            assertThat(secondResult).isNull();
            verify(redisTemplate, times(1)).delete("oauth_state:one-time-state");
        }
    }

    @Nested
    @DisplayName("cleanupExpiredStates()")
    class CleanupExpiredStatesTests {

        @Test
        @DisplayName("should execute without errors")
        void cleanupExpiredStates_Success() {
            // Act & Assert - Just verify it doesn't throw
            assertThatCode(() -> stateService.cleanupExpiredStates())
                .doesNotThrowAnyException();
        }
    }
}
