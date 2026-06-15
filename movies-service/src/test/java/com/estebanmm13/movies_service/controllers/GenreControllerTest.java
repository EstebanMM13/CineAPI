package com.estebanmm13.movies_service.controllers;

import com.estebanmm13.movies_service.config.JwtService;
import com.estebanmm13.movies_service.dtoModels.response.GenreResponseDTO;
import com.estebanmm13.movies_service.error.notFound.GenreNotFoundException;
import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.services.genre.GenreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.estebanmm13.movies_service.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GenreController.class)
@Import(SecurityConfig.class)
class GenreControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private GenreService genreService;
    @MockitoBean private JwtService jwtService;

    private static final String BASE_URL = "/api/genres";

    private GenreResponseDTO dto(Long id, String name) {
        return new GenreResponseDTO(id, name);
    }

    private Genre genreBody(String name) {
        return Genre.builder().name(name).build();
    }

    // ── GET /api/genres ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAllGenres_authenticated_returns200() throws Exception {
        Page<GenreResponseDTO> page = new PageImpl<>(List.of(dto(1L, "Action"), dto(2L, "Drama")));
        given(genreService.findAllGenres(any())).willReturn(page);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Action"));
    }

    @Test
    void findAllGenres_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/genres/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findGenreById_existingId_returns200() throws Exception {
        given(genreService.findGenreById(1L)).willReturn(dto(1L, "Action"));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Action"));
    }

    @Test
    @WithMockUser
    void findGenreById_notFound_returns404() throws Exception {
        given(genreService.findGenreById(99L))
                .willThrow(new GenreNotFoundException("Genre not found with id: 99"));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/genres ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGenre_asAdmin_returns201() throws Exception {
        Genre body = genreBody("Horror");
        Genre saved = Genre.builder().id(1L).name("Horror").build();
        given(genreService.createGenre(any())).willReturn(saved);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Horror"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createGenre_asUser_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("Horror"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGenre_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("Horror"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGenre_blankName_returns400() throws Exception {
        Genre invalid = Genre.builder().name("").build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGenre_duplicateName_returns400() throws Exception {
        given(genreService.createGenre(any()))
                .willThrow(new IllegalArgumentException("Genre already exists with name: Action"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("Action"))))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/genres/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateGenre_asAdmin_returns200() throws Exception {
        Genre saved = Genre.builder().id(1L).name("Thriller").build();
        given(genreService.updateGenre(eq(1L), any())).willReturn(saved);

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("Thriller"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thriller"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateGenre_notFound_returns404() throws Exception {
        given(genreService.updateGenre(eq(99L), any()))
                .willThrow(new GenreNotFoundException("Genre not found with id: 99"));

        mockMvc.perform(patch(BASE_URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateGenre_asUser_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genreBody("X"))))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/genres/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteGenre_asAdmin_returns204() throws Exception {
        willDoNothing().given(genreService).deleteGenre(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteGenre_notFound_returns404() throws Exception {
        willThrow(new GenreNotFoundException("Genre not found with id: 99"))
                .given(genreService).deleteGenre(99L);

        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteGenre_asUser_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/genres/name/{name} ───────────────────────────────────────────

    @Test
    @WithMockUser
    void findGenreByName_withName_returns200() throws Exception {
        Page<GenreResponseDTO> page = new PageImpl<>(List.of(dto(1L, "Action")));
        given(genreService.findGenreByName(eq("act"), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/name/act"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Action"));
    }
}
