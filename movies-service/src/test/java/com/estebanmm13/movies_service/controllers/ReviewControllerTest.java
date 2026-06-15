package com.estebanmm13.movies_service.controllers;

import com.estebanmm13.movies_service.config.JwtService;
import com.estebanmm13.movies_service.dtoModels.request.ReviewRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.error.notFound.ReviewNotFoundException;
import com.estebanmm13.movies_service.services.review.ReviewService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtService jwtService;

    private static final String BASE_URL = "/api/movies/{movieId}/reviews";

    private ReviewResponseDTO reviewDto(Long id) {
        return ReviewResponseDTO.builder()
                .id(id)
                .comment("Great film!")
                .createdAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .userId(10L)
                .movieTitle("Inception")
                .build();
    }

    private ReviewRequestDTO reviewRequest(String comment) {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setComment(comment);
        return dto;
    }

    // ── GET /api/movies/{movieId}/reviews ─────────────────────────────────────

    @Test
    @WithMockUser
    void findReviewsByMovie_authenticated_returns200() throws Exception {
        Page<ReviewResponseDTO> page = new PageImpl<>(List.of(reviewDto(1L)));
        given(reviewService.findReviewsByMovieId(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/api/movies/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void findReviewsByMovie_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/movies/1/reviews"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/movies/{movieId}/reviews/{id} ────────────────────────────────

    @Test
    @WithMockUser
    void findReviewById_existingId_returns200() throws Exception {
        given(reviewService.findReviewById(1L)).willReturn(reviewDto(1L));

        mockMvc.perform(get("/api/movies/1/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.comment").value("Great film!"));
    }

    @Test
    @WithMockUser
    void findReviewById_notFound_returns404() throws Exception {
        given(reviewService.findReviewById(99L))
                .willThrow(new ReviewNotFoundException("Review with ID 99 not found"));

        mockMvc.perform(get("/api/movies/1/reviews/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/movies/{movieId}/reviews ────────────────────────────────────

    @Test
    @WithMockUser
    void createReview_validRequest_returns201() throws Exception {
        ReviewRequestDTO req = reviewRequest("Amazing movie!");
        given(reviewService.createReview(eq(10L), eq(1L), any())).willReturn(reviewDto(1L));

        mockMvc.perform(post("/api/movies/1/reviews")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void createReview_blankComment_returns400() throws Exception {
        ReviewRequestDTO invalid = reviewRequest("");

        mockMvc.perform(post("/api/movies/1/reviews")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createReview_movieNotFound_returns404() throws Exception {
        given(reviewService.createReview(anyLong(), eq(99L), any()))
                .willThrow(new MovieNotFoundException("Movie with ID 99 not found"));

        mockMvc.perform(post("/api/movies/99/reviews")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest("comment"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createReview_duplicateReview_returns500() throws Exception {
        given(reviewService.createReview(anyLong(), eq(1L), any()))
                .willThrow(new RuntimeException("User already submitted a review for this movie"));

        mockMvc.perform(post("/api/movies/1/reviews")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest("comment"))))
                .andExpect(status().isInternalServerError());
    }

    // ── PATCH /api/movies/{movieId}/reviews/{id} ──────────────────────────────

    @Test
    @WithMockUser
    void updateReview_owner_returns200() throws Exception {
        given(reviewService.updateReview(eq(1L), eq(10L), any())).willReturn(reviewDto(1L));

        mockMvc.perform(patch("/api/movies/1/reviews/1")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest("Updated!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void updateReview_notOwner_returns404() throws Exception {
        given(reviewService.updateReview(eq(1L), eq(99L), any()))
                .willThrow(new ReviewNotFoundException("You cannot delete this review"));

        mockMvc.perform(patch("/api/movies/1/reviews/1")
                        .param("userId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest("hack"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateReview_blankComment_returns400() throws Exception {
        mockMvc.perform(patch("/api/movies/1/reviews/1")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/movies/{movieId}/reviews/{id} ─────────────────────────────

    @Test
    @WithMockUser
    void deleteReview_owner_returns204() throws Exception {
        willDoNothing().given(reviewService).deleteReview(1L, 10L);

        mockMvc.perform(delete("/api/movies/1/reviews/1")
                        .param("userId", "10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteReview_notOwner_returns404() throws Exception {
        willThrow(new ReviewNotFoundException("You cannot delete this review"))
                .given(reviewService).deleteReview(1L, 99L);

        mockMvc.perform(delete("/api/movies/1/reviews/1")
                        .param("userId", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteReview_notFound_returns404() throws Exception {
        willThrow(new ReviewNotFoundException("Review with ID 99 not found"))
                .given(reviewService).deleteReview(99L, 10L);

        mockMvc.perform(delete("/api/movies/1/reviews/99")
                        .param("userId", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/movies/1/reviews/1")
                        .param("userId", "10"))
                .andExpect(status().isUnauthorized());
    }
}
