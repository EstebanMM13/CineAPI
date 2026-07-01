package com.estebanmm13.movies_service.controllers;

import com.estebanmm13.movies_service.config.JwtService;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.mapper.ReviewMapper;
import com.estebanmm13.movies_service.services.review.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.estebanmm13.movies_service.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewUsersController.class)
@Import(SecurityConfig.class)
class ReviewUsersControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private ReviewMapper reviewMapper;
    @MockitoBean private JwtService jwtService;

    private static final String BASE_URL = "/api/reviews";

    private ReviewResponseDTO reviewDto(Long id) {
        return ReviewResponseDTO.builder()
                .id(id)
                .comment("Enjoyed it!")
                .createdAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .userId(10L)
                .movieTitle("Inception")
                .build();
    }

    // ── GET /api/reviews/{userId} ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void findReviewsByUser_authenticated_returns200WithPage() throws Exception {
        Page<ReviewResponseDTO> page = new PageImpl<>(List.of(reviewDto(1L), reviewDto(2L)));
        given(reviewService.findReviewsByUserId(eq(10L), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].userId").value(10))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser
    void findReviewsByUser_noReviews_returnsEmptyPage() throws Exception {
        given(reviewService.findReviewsByUserId(eq(99L), any())).willReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void findReviewsByUser_unauthenticated_returns200() throws Exception {
        Page<ReviewResponseDTO> page = new PageImpl<>(List.of(reviewDto(1L), reviewDto(2L)));
        given(reviewService.findReviewsByUserId(eq(10L), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL + "/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser
    void findReviewsByUser_withPaginationParams_passesThroughCorrectly() throws Exception {
        given(reviewService.findReviewsByUserId(eq(10L), any())).willReturn(Page.empty());

        mockMvc.perform(get(BASE_URL + "/10")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }
}
