package com.estebanmm13.auth_service.controllers;

import com.estebanmm13.auth_service.config.JwtService;
import com.estebanmm13.auth_service.dtoModels.response.SystemStats;
import com.estebanmm13.auth_service.dtoModels.response.UserResponseDTO;
import com.estebanmm13.auth_service.error.UserNotFoundException;
import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.services.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.estebanmm13.auth_service.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String BASE_URL = "/api/admin";

    private UserResponseDTO userDto(Long id) {
        return new UserResponseDTO(id, "user" + id, "user" + id + "@example.com", "USER");
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_returns200WithPage() throws Exception {
        Page<UserResponseDTO> page = new PageImpl<>(List.of(userDto(1L), userDto(2L)));
        given(userService.findAllUsers(any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_withPaginationParams_passesThemToService() throws Exception {
        given(userService.findAllUsers(any())).willReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "username")
                        .param("direction", "desc"))
                .andExpect(status().isOk());

        then(userService).should().findAllUsers(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/admin/users/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_existingId_returns200() throws Exception {
        given(userService.findUserById(1L)).willReturn(userDto(1L));

        mockMvc.perform(get(BASE_URL + "/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_notFound_returns404() throws Exception {
        given(userService.findUserById(99L))
                .willThrow(new UserNotFoundException("User with ID 99 not found"));

        mockMvc.perform(get(BASE_URL + "/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserById_asUser_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users/1"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/admin/users/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_existingId_returns204() throws Exception {
        willDoNothing().given(userService).deleteUser(1L);

        mockMvc.perform(delete(BASE_URL + "/users/1"))
                .andExpect(status().isNoContent());

        then(userService).should().deleteUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_notFound_returns404() throws Exception {
        willThrow(new UserNotFoundException("User with ID 99 not found"))
                .given(userService).deleteUser(99L);

        mockMvc.perform(delete(BASE_URL + "/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/users/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/admin/stats ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSystemStats_asAdmin_returns200WithStats() throws Exception {
        given(userService.countUsers()).willReturn(10L);
        given(userService.countUsersByRole(Role.ADMIN)).willReturn(2L);
        given(userService.countUsersByRole(Role.USER)).willReturn(8L);

        mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.adminUsers").value(2))
                .andExpect(jsonPath("$.regularUsers").value(8));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getSystemStats_asUser_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isForbidden());
    }
}
