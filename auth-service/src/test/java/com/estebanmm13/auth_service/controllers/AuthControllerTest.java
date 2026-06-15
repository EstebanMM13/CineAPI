package com.estebanmm13.auth_service.controllers;

import com.estebanmm13.auth_service.config.JwtService;
import com.estebanmm13.auth_service.dtoModels.request.AuthenticationRequest;
import com.estebanmm13.auth_service.dtoModels.request.RegisterRequest;
import com.estebanmm13.auth_service.dtoModels.response.AuthResponse;
import com.estebanmm13.auth_service.error.DuplicateResourceException;
import com.estebanmm13.auth_service.error.InvalidCredentialsException;
import com.estebanmm13.auth_service.services.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.estebanmm13.auth_service.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private static final String REGISTER_URL    = "/api/auth/register";
    private static final String AUTHENTICATE_URL = "/api/auth/authenticate";

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("secret123")
                .build();
        given(authService.register(any())).willReturn(AuthResponse.builder().token("jwt-token").build());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("")
                .email("john@example.com")
                .password("secret123")
                .build();

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameTooShort_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("ab")
                .email("john@example.com")
                .password("secret123")
                .build();

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("johndoe")
                .email("not-valid-email")
                .password("secret123")
                .build();

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordTooShort_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("abc")
                .build();

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("secret123")
                .build();
        willThrow(new DuplicateResourceException("User", "email", "john@example.com"))
                .given(authService).register(any());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("secret123")
                .build();
        willThrow(new DuplicateResourceException("User", "username", "johndoe"))
                .given(authService).register(any());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_missingBody_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/authenticate ───────────────────────────────────────────

    @Test
    void authenticate_validCredentials_returns200WithToken() throws Exception {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "secret123");
        given(authService.authenticate(any())).willReturn(AuthResponse.builder().token("jwt-token").build());

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void authenticate_wrongPassword_returns401() throws Exception {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "wrong");
        willThrow(new InvalidCredentialsException("User or password incorrect"))
                .given(authService).authenticate(any());

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_blankUsername_returns400() throws Exception {
        AuthenticationRequest req = new AuthenticationRequest("", "secret123");

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_blankPassword_returns400() throws Exception {
        AuthenticationRequest req = new AuthenticationRequest("johndoe", "");

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
