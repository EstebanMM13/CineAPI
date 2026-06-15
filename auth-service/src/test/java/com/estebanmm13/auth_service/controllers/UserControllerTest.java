package com.estebanmm13.auth_service.controllers;

import com.estebanmm13.auth_service.config.JwtService;
import com.estebanmm13.auth_service.dtoModels.response.UserResponseDTO;
import com.estebanmm13.auth_service.error.UserNotFoundException;
import com.estebanmm13.auth_service.services.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.estebanmm13.auth_service.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String BASE_URL = "/api/users";

    // ── GET /api/users/{id}/username ──────────────────────────────────────────

    @Test
    @WithMockUser
    void getUsernameById_existingUser_returns200WithUsername() throws Exception {
        UserResponseDTO dto = new UserResponseDTO(1L, "johndoe", "john@example.com", "USER");
        given(userService.findUserById(1L)).willReturn(dto);

        mockMvc.perform(get(BASE_URL + "/1/username"))
                .andExpect(status().isOk())
                .andExpect(content().string("johndoe"));
    }

    @Test
    @WithMockUser
    void getUsernameById_notFound_returns404() throws Exception {
        given(userService.findUserById(99L))
                .willThrow(new UserNotFoundException("User with ID 99 not found"));

        mockMvc.perform(get(BASE_URL + "/99/username"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsernameById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1/username"))
                .andExpect(status().isUnauthorized());
    }
}
