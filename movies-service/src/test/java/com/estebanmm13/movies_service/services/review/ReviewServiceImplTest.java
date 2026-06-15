package com.estebanmm13.movies_service.services.review;

import com.estebanmm13.movies_service.dtoModels.request.ReviewRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.error.notFound.ReviewNotFoundException;
import com.estebanmm13.movies_service.mapper.ReviewMapper;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Review;
import com.estebanmm13.movies_service.repositories.MovieRepository;
import com.estebanmm13.movies_service.repositories.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks private ReviewServiceImpl reviewService;

    private Movie movie(Long id) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle("Movie " + id);
        m.setGenres(List.of());
        return m;
    }

    private Review review(Long id, Long userId, Movie movie) {
        Review r = new Review();
        r.setId(id);
        r.setUserId(userId);
        r.setMovie(movie);
        r.setComment("Great movie!");
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private ReviewResponseDTO reviewDto(Long id) {
        return ReviewResponseDTO.builder()
                .id(id)
                .comment("Great movie!")
                .createdAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .userId(1L)
                .movieTitle("Movie 1")
                .build();
    }

    private ReviewRequestDTO reviewRequest(String comment) {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setComment(comment);
        return dto;
    }

    // ── findReviewById ────────────────────────────────────────────────────────

    @Test
    void findReviewById_existingId_returnsDTO() {
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);

        given(reviewRepository.findById(1L)).willReturn(Optional.of(r));
        given(reviewMapper.toResponseDTO(r)).willReturn(reviewDto(1L));

        ReviewResponseDTO result = reviewService.findReviewById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findReviewById_notFound_throwsReviewNotFoundException() {
        given(reviewRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findReviewById(99L))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    // ── createReview ──────────────────────────────────────────────────────────

    @Test
    void createReview_happyPath_savesAndReturnsDTO() {
        Movie m = movie(1L);
        ReviewRequestDTO req = reviewRequest("Awesome film!");
        Review entity = review(1L, 10L, m);
        ReviewResponseDTO expected = reviewDto(1L);

        given(movieRepository.findById(1L)).willReturn(Optional.of(m));
        given(reviewRepository.existsByUserIdAndMovieId(10L, 1L)).willReturn(false);
        given(reviewMapper.toEntity(req, 10L, m)).willReturn(entity);
        given(reviewRepository.save(entity)).willReturn(entity);
        given(reviewMapper.toResponseDTO(entity)).willReturn(expected);

        ReviewResponseDTO result = reviewService.createReview(10L, 1L, req);

        assertThat(result).isEqualTo(expected);
        then(reviewRepository).should().save(entity);
    }

    @Test
    void createReview_movieNotFound_throwsMovieNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(10L, 99L, reviewRequest("comment")))
                .isInstanceOf(MovieNotFoundException.class);

        then(reviewRepository).should(never()).save(any());
    }

    @Test
    void createReview_duplicateReview_throwsRuntimeException() {
        Movie m = movie(1L);
        given(movieRepository.findById(1L)).willReturn(Optional.of(m));
        given(reviewRepository.existsByUserIdAndMovieId(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(10L, 1L, reviewRequest("comment")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already submitted");

        then(reviewRepository).should(never()).save(any());
    }

    // ── updateReview ──────────────────────────────────────────────────────────

    @Test
    void updateReview_ownerUpdates_returnsUpdatedDTO() {
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        ReviewRequestDTO req = reviewRequest("Updated comment");
        ReviewResponseDTO expected = reviewDto(1L);

        given(reviewRepository.findById(1L)).willReturn(Optional.of(r));
        given(reviewRepository.save(r)).willReturn(r);
        given(reviewMapper.toResponseDTO(r)).willReturn(expected);

        ReviewResponseDTO result = reviewService.updateReview(1L, 10L, req);

        assertThat(result).isEqualTo(expected);
        assertThat(r.getComment()).isEqualTo("Updated comment");
    }

    @Test
    void updateReview_notOwner_throwsReviewNotFoundException() {
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> reviewService.updateReview(1L, 99L, reviewRequest("hack")))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("cannot");

        then(reviewRepository).should(never()).save(any());
    }

    @Test
    void updateReview_notFound_throwsReviewNotFoundException() {
        given(reviewRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(99L, 10L, reviewRequest("comment")))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    // ── deleteReview ──────────────────────────────────────────────────────────

    @Test
    void deleteReview_ownerDeletes_callsDeleteById() {
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(r));

        reviewService.deleteReview(1L, 10L);

        then(reviewRepository).should().deleteById(1L);
    }

    @Test
    void deleteReview_notOwner_throwsReviewNotFoundException() {
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        given(reviewRepository.findById(1L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 99L))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("cannot");

        then(reviewRepository).should(never()).deleteById(any());
    }

    @Test
    void deleteReview_notFound_throwsReviewNotFoundException() {
        given(reviewRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(99L, 10L))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    // ── findReviewsByMovieId ──────────────────────────────────────────────────

    @Test
    void findReviewsByMovieId_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        Page<Review> page = new PageImpl<>(List.of(r));

        given(reviewRepository.findReviewsByMovieId(1L, pageable)).willReturn(page);
        given(reviewMapper.toResponseDTO(r)).willReturn(reviewDto(1L));

        Page<ReviewResponseDTO> result = reviewService.findReviewsByMovieId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── findReviewsByUserId ───────────────────────────────────────────────────

    @Test
    void findReviewsByUserId_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie m = movie(1L);
        Review r = review(1L, 10L, m);
        Page<Review> page = new PageImpl<>(List.of(r));

        given(reviewRepository.findReviewsByUserId(10L, pageable)).willReturn(page);
        given(reviewMapper.toResponseDTO(r)).willReturn(reviewDto(1L));

        Page<ReviewResponseDTO> result = reviewService.findReviewsByUserId(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
