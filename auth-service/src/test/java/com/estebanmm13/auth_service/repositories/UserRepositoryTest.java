package com.estebanmm13.auth_service.repositories;

import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = em.persistFlushFind(User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password("$2a$10$hashed")
                .role(Role.ADMIN)
                .build());

        regularUser = em.persistFlushFind(User.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("$2a$10$hashed")
                .role(Role.USER)
                .build());
    }

    // ── findUserByUsername ────────────────────────────────────────────────────

    @Test
    void findUserByUsername_existingUsername_returnsUser() {
        Optional<User> result = userRepository.findUserByUsername("johndoe");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findUserByUsername_nonExisting_returnsEmpty() {
        Optional<User> result = userRepository.findUserByUsername("nobody");

        assertThat(result).isEmpty();
    }

    // ── findUserByUsernameIgnoreCase ──────────────────────────────────────────

    @Test
    void findUserByUsernameIgnoreCase_upperCase_returnsUser() {
        Optional<User> result = userRepository.findUserByUsernameIgnoreCase("JOHNDOE");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void findUserByUsernameIgnoreCase_mixedCase_returnsUser() {
        Optional<User> result = userRepository.findUserByUsernameIgnoreCase("JohnDoe");

        assertThat(result).isPresent();
    }

    // ── findUserByUsernameIgnoreCaseContaining ────────────────────────────────

    @Test
    void findUserByUsernameIgnoreCaseContaining_partialMatch_returnsUser() {
        Optional<User> result = userRepository.findUserByUsernameIgnoreCaseContaining("JOHN");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void findUserByUsernameIgnoreCaseContaining_noMatch_returnsEmpty() {
        Optional<User> result = userRepository.findUserByUsernameIgnoreCaseContaining("xyz999");

        assertThat(result).isEmpty();
    }

    // ── findUserByEmail ───────────────────────────────────────────────────────

    @Test
    void findUserByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findUserByEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void findUserByEmail_nonExisting_returnsEmpty() {
        Optional<User> result = userRepository.findUserByEmail("ghost@example.com");

        assertThat(result).isEmpty();
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    void existsByEmail_nonExisting_returnsFalse() {
        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    // ── existsByUsername ──────────────────────────────────────────────────────

    @Test
    void existsByUsername_existingUsername_returnsTrue() {
        assertThat(userRepository.existsByUsername("johndoe")).isTrue();
    }

    @Test
    void existsByUsername_nonExisting_returnsFalse() {
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    // ── countByRole ───────────────────────────────────────────────────────────

    @Test
    void countByRole_userRole_returnsCorrectCount() {
        assertThat(userRepository.countByRole(Role.USER)).isEqualTo(1L);
    }

    @Test
    void countByRole_adminRole_returnsCorrectCount() {
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1L);
    }

    @Test
    void countByRole_afterSavingExtraUser_incrementsCount() {
        em.persistAndFlush(User.builder()
                .username("anotheruser")
                .email("another@example.com")
                .password("$2a$10$hashed")
                .role(Role.USER)
                .build());

        assertThat(userRepository.countByRole(Role.USER)).isEqualTo(2L);
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    void save_newUser_persistsSuccessfully() {
        User newUser = User.builder()
                .username("newuser")
                .email("new@example.com")
                .password("$2a$10$hashed")
                .role(Role.USER)
                .build();

        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_existingUser_removesFromDatabase() {
        userRepository.delete(regularUser);
        em.flush();

        assertThat(userRepository.findById(regularUser.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllUsers() {
        assertThat(userRepository.findAll()).hasSize(2);
    }
}
