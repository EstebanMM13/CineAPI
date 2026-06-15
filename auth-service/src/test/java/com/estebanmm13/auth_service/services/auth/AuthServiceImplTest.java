package com.estebanmm13.auth_service.services.auth;

import com.estebanmm13.auth_service.config.JwtService;
import com.estebanmm13.auth_service.dtoModels.request.AuthenticationRequest;
import com.estebanmm13.auth_service.dtoModels.request.RegisterRequest;
import com.estebanmm13.auth_service.dtoModels.response.AuthResponse;
import com.estebanmm13.auth_service.error.DuplicateResourceException;
import com.estebanmm13.auth_service.error.InvalidCredentialsException;
import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.models.User;
import com.estebanmm13.auth_service.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthServiceImpl authService;

    private RegisterRequest registerRequest() {
        return RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("secret123")
                .build();
    }

    private User savedUser() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .password("$2a$10$encoded")
                .role(Role.USER)
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_returnsJwtToken() {
        RegisterRequest req = registerRequest();
        User saved = savedUser();

        given(userRepository.existsByEmail(req.getEmail())).willReturn(false);
        given(userRepository.existsByUsername(req.getUsername())).willReturn(false);
        given(passwordEncoder.encode(req.getPassword())).willReturn("$2a$10$encoded");
        given(userRepository.save(any(User.class))).willReturn(saved);
        given(jwtService.generateTokenWithRole(saved, "USER")).willReturn("jwt-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void register_newUserHasRoleUser() {
        RegisterRequest req = registerRequest();
        User saved = savedUser();

        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.existsByUsername(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getRole()).isEqualTo(Role.USER);
            return saved;
        });
        given(jwtService.generateTokenWithRole(any(), any())).willReturn("token");

        authService.register(req);
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest req = registerRequest();
        given(userRepository.existsByEmail(req.getEmail())).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        then(userRepository).should(never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsDuplicateResourceException() {
        RegisterRequest req = registerRequest();
        given(userRepository.existsByEmail(req.getEmail())).willReturn(false);
        given(userRepository.existsByUsername(req.getUsername())).willReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

        then(userRepository).should(never()).save(any());
    }

    @Test
    void register_passwordIsEncoded() {
        RegisterRequest req = registerRequest();
        User saved = savedUser();

        given(userRepository.existsByEmail(any())).willReturn(false);
        given(userRepository.existsByUsername(any())).willReturn(false);
        given(passwordEncoder.encode("secret123")).willReturn("$2a$10$hashed");
        given(userRepository.save(any(User.class))).willReturn(saved);
        given(jwtService.generateTokenWithRole(any(), any())).willReturn("token");

        authService.register(req);

        then(passwordEncoder).should().encode("secret123");
    }

    // ── authenticate ─────────────────────────────────────────────────────────

    @Test
    void authenticate_validCredentials_returnsJwtToken() {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "secret123");
        User user = savedUser();

        given(userRepository.findUserByUsername("johndoe")).willReturn(Optional.of(user));
        given(jwtService.generateTokenWithRole(user, "USER")).willReturn("jwt-token");

        AuthResponse response = authService.authenticate(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_badCredentials_throwsInvalidCredentialsException() {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "wrong");

        willThrow(BadCredentialsException.class).given(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.authenticate(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_authenticatesWithCorrectCredentials() {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "secret123");
        User user = savedUser();

        given(userRepository.findUserByUsername("johndoe")).willReturn(Optional.of(user));
        given(jwtService.generateTokenWithRole(any(), any())).willReturn("token");

        authService.authenticate(req);

        then(authenticationManager).should().authenticate(
                argThat(a -> a instanceof UsernamePasswordAuthenticationToken
                        && "johndoe".equals(a.getPrincipal())
                        && "secret123".equals(a.getCredentials()))
        );
    }
}
