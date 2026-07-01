package com.estebanmm13.movies_service.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsernameResolver {

    private static final String UNKNOWN_USERNAME = "Usuario desconocido";

    private final AuthServiceClient authServiceClient;

    @Cacheable(value = "usernames", key = "#userId")
    @CircuitBreaker(name = "auth-service-cb", fallbackMethod = "usernameFallback")
    @Retry(name = "auth-service-retry", fallbackMethod = "usernameFallback")
    public String resolveUsername(Long userId) {
        return authServiceClient.getUsernameById(userId);
    }

    public String usernameFallback(Long userId, Exception ex) {
        log.warn("Fallback activado para userId {}: {}", userId, ex.getMessage());
        return UNKNOWN_USERNAME;
    }
}
