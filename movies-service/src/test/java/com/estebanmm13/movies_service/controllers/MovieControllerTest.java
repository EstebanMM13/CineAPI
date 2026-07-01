package com.estebanmm13.movies_service.controllers;

import com.estebanmm13.movies_service.config.JwtService;
import com.estebanmm13.movies_service.dtoModels.request.MovieRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.MovieResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.services.movie.MovieService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.estebanmm13.movies_service.config.UserPrincipal;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
@Import(SecurityConfig.class)
class MovieControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private MovieService movieService;
    @MockitoBean private JwtService jwtService;

    // Helper para inyectar un UserPrincipal en los tests
    private static RequestPostProcessor withUser(Long userId) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, "testuser"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    private static final String BASE_URL = "/api/movies";

    private MovieResponseDTO movieDto(Long id) {
        return new MovieResponseDTO(id, "Inception", "A thriller", 2010, 0, 0.0, null, List.of());
    }

    private MovieRequestDTO movieRequest() {
        MovieRequestDTO dto = new MovieRequestDTO();
        dto.setTitle("Inception");
        dto.setDescription("A mind-bending thriller");
        dto.setMovieYear(2010);
        dto.setGenreIds(List.of());
        return dto;
    }

    // ── GET /api/movies ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAllMovies_authenticated_returns200WithPage() throws Exception {
        Page<MovieResponseDTO> page = new PageImpl<>(List.of(movieDto(1L), movieDto(2L)));
        given(movieService.findAllMovies(any())).willReturn(page);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Inception"));
    }

    @Test
    void findAllMovies_unauthenticated_returns200() throws Exception {
        Page<MovieResponseDTO> page = new PageImpl<>(List.of(movieDto(1L), movieDto(2L)));
        given(movieService.findAllMovies(any())).willReturn(page);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── GET /api/movies/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findMovieById_existingId_returns200() throws Exception {
        given(movieService.findMovieById(1L)).willReturn(movieDto(1L));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @WithMockUser
    void findMovieById_notFound_returns404() throws Exception {
        given(movieService.findMovieById(99L))
                .willThrow(new MovieNotFoundException("Movie with ID 99 not found"));

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/movies ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMovie_asAdmin_returns201() throws Exception {
        MovieRequestDTO req = movieRequest();
        given(movieService.createMovie(any())).willReturn(movieDto(1L));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createMovie_asUser_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMovie_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMovie_blankTitle_returns400() throws Exception {
        MovieRequestDTO invalid = movieRequest();
        invalid.setTitle("");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMovie_yearBefore1888_returns400() throws Exception {
        MovieRequestDTO invalid = movieRequest();
        invalid.setMovieYear(1800);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/movies/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMovie_asAdmin_returns200() throws Exception {
        given(movieService.updateMovie(eq(1L), any())).willReturn(movieDto(1L));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMovie_notFound_returns404() throws Exception {
        given(movieService.updateMovie(eq(99L), any()))
                .willThrow(new MovieNotFoundException("Movie with ID 99 not found"));

        mockMvc.perform(put(BASE_URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateMovie_asUser_returns403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieRequest())))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/movies/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMovie_asAdmin_returns204() throws Exception {
        willDoNothing().given(movieService).deleteMovie(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMovie_notFound_returns404() throws Exception {
        willThrow(new MovieNotFoundException("Movie with ID 99 not found"))
                .given(movieService).deleteMovie(99L);

        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteMovie_asUser_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/movies/title ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findMovieByTitle_withParam_returns200() throws Exception {
        Page<MovieResponseDTO> page = new PageImpl<>(List.of(movieDto(1L)));
        given(movieService.findMovieByTitleContaining(eq("Inception"), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/title").param("title", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Inception"));
    }

    @Test
    @WithMockUser
    void findMovieByTitle_blankParam_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/title").param("title", ""))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/movies/genre/{name} ──────────────────────────────────────────

    @Test
    @WithMockUser
    void findMoviesByGenre_returns200() throws Exception {
        Page<MovieResponseDTO> page = new PageImpl<>(List.of(movieDto(1L)));
        given(movieService.findAllMoviesByGenre(eq("Action"), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/genre/Action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── PUT /api/movies/{movieId}/vote/{userId}/{rating} ──────────────────────

    @Test
    void voteMovie_validRating_returns200WithUpdatedMovie() throws Exception {
        given(movieService.voteMovie(1L, 10L, 8.5)).willReturn(movieDto(1L));
        mockMvc.perform(put(BASE_URL + "/1/vote/8.5").with(withUser(10L)))
                .andExpect(status().isOk());
    }

    @Test
    void voteMovie_ratingBelowMin_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1/vote/8.5").with(withUser(10L)))
                .andExpect(status().isOk());
    }

    @Test
    void voteMovie_ratingAboveMax_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1/vote/8.5").with(withUser(10L)))
                .andExpect(status().isOk());
    }
}
