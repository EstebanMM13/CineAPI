package com.estebanmm13.auth_service.config;

import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "2da08e8ee264a149413ce46abebbd566b4c8bbb019b37708ed9d77f8511ac7bf";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION_TIME", 86400000L);
    }

    private User user(Role role) {
        return User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(role)
                .build();
    }

    @Test
    void generateTokenWithRole_createsValidToken() {
        String token = jwtService.generateTokenWithRole(user(Role.USER), "USER");

        assertThat(token).isNotBlank();
    }

    @Test
    void getUserName_returnsCorrectSubject() {
        String token = jwtService.generateTokenWithRole(user(Role.USER), "USER");

        assertThat(jwtService.getUserName(token)).isEqualTo("testuser");
    }

    @Test
    void extractRole_returnsEmbeddedRole() {
        String token = jwtService.generateTokenWithRole(user(Role.USER), "USER");

        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void extractRole_adminToken_returnsAdmin() {
        String token = jwtService.generateTokenWithRole(user(Role.ADMIN), "ADMIN");

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        User u = user(Role.USER);
        String token = jwtService.generateTokenWithRole(u, "USER");

        assertThat(jwtService.validateToken(token, u)).isTrue();
    }

    @Test
    void validateToken_differentUser_returnsFalse() {
        User owner = user(Role.USER);
        User other = User.builder().username("other").password("x").role(Role.USER).build();
        String token = jwtService.generateTokenWithRole(owner, "USER");

        assertThat(jwtService.validateToken(token, other)).isFalse();
    }

    @Test
    void getAuthorities_userRole_returnsRoleUser() {
        String token = jwtService.generateTokenWithRole(user(Role.USER), "USER");

        List<GrantedAuthority> authorities = jwtService.getAuthorities(token);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void getAuthorities_adminRole_returnsRoleAdmin() {
        String token = jwtService.generateTokenWithRole(user(Role.ADMIN), "ADMIN");

        List<GrantedAuthority> authorities = jwtService.getAuthorities(token);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void generateToken_withoutRole_hasNoRoleClaim() {
        String token = jwtService.generateToken(user(Role.USER));

        assertThat(jwtService.extractRole(token)).isNull();
    }
}
