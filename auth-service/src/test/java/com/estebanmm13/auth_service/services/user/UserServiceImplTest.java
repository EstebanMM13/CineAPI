package com.estebanmm13.auth_service.services.user;

import com.estebanmm13.auth_service.dtoModels.response.UserResponseDTO;
import com.estebanmm13.auth_service.error.UserNotFoundException;
import com.estebanmm13.auth_service.mapper.UserMapper;
import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.models.User;
import com.estebanmm13.auth_service.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@example.com")
                .password("encoded")
                .role(role)
                .build();
    }

    private UserResponseDTO dto(Long id) {
        return new UserResponseDTO(id, "user" + id, "user" + id + "@example.com", "USER");
    }

    // ── findAllUsers ──────────────────────────────────────────────────────────

    @Test
    void findAllUsers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User u = user(1L, Role.USER);
        UserResponseDTO expected = dto(1L);
        Page<User> userPage = new PageImpl<>(List.of(u));

        given(userRepository.findAll(pageable)).willReturn(userPage);
        given(userMapper.toResponseDTO(u)).willReturn(expected);

        Page<UserResponseDTO> result = userService.findAllUsers(pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void findAllUsers_emptyRepository_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        given(userRepository.findAll(pageable)).willReturn(Page.empty());

        Page<UserResponseDTO> result = userService.findAllUsers(pageable);

        assertThat(result).isEmpty();
    }

    // ── findUserById ──────────────────────────────────────────────────────────

    @Test
    void findUserById_existingId_returnsDTO() {
        User u = user(1L, Role.USER);
        UserResponseDTO expected = dto(1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userMapper.toResponseDTO(u)).willReturn(expected);

        UserResponseDTO result = userService.findUserById(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findUserById_notFound_throwsUserNotFoundException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_existingId_callsDelete() {
        given(userRepository.existsById(1L)).willReturn(true);

        userService.deleteUser(1L);

        then(userRepository).should().deleteById(1L);
    }

    @Test
    void deleteUser_notFound_throwsException() {
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class);

        then(userRepository).should(never()).deleteById(any());
    }

    // ── countUsers ────────────────────────────────────────────────────────────

    @Test
    void countUsers_returnsRepositoryCount() {
        given(userRepository.count()).willReturn(42L);

        assertThat(userService.countUsers()).isEqualTo(42L);
    }

    // ── countUsersByRole ──────────────────────────────────────────────────────

    @Test
    void countUsersByRole_adminRole_returnsCount() {
        given(userRepository.countByRole(Role.ADMIN)).willReturn(3L);

        assertThat(userService.countUsersByRole(Role.ADMIN)).isEqualTo(3L);
    }

    @Test
    void countUsersByRole_userRole_returnsCount() {
        given(userRepository.countByRole(Role.USER)).willReturn(39L);

        assertThat(userService.countUsersByRole(Role.USER)).isEqualTo(39L);
    }

    // ── findUserByEmail ───────────────────────────────────────────────────────

    @Test
    void findUserByEmail_existingEmail_returnsDTO() {
        User u = user(1L, Role.USER);
        UserResponseDTO expected = dto(1L);

        given(userRepository.findUserByEmail("user1@example.com")).willReturn(Optional.of(u));
        given(userMapper.toResponseDTO(u)).willReturn(expected);

        UserResponseDTO result = userService.findUserByEmail("user1@example.com");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findUserByEmail_notFound_throwsUserNotFoundException() {
        given(userRepository.findUserByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserByEmail("nobody@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── existsUserByEmail ─────────────────────────────────────────────────────

    @Test
    void existsUserByEmail_existing_returnsTrue() {
        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThat(userService.existsUserByEmail("john@example.com")).isTrue();
    }

    @Test
    void existsUserByEmail_missing_returnsFalse() {
        given(userRepository.existsByEmail("ghost@example.com")).willReturn(false);

        assertThat(userService.existsUserByEmail("ghost@example.com")).isFalse();
    }
}
