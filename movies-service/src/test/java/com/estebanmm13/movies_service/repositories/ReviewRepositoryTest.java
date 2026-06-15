package com.estebanmm13.movies_service.repositories;

import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ReviewRepository reviewRepository;

    private Movie inception;
    private Movie theGodfather;
    private Review review1;
    private Review review2;
    private Review review3;

    @BeforeEach
    void setUp() {
        inception = em.persistFlushFind(Movie.builder()
                .title("Inception")
                .description("A thief who steals corporate secrets")
                .movieYear(2010)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build());

        theGodfather = em.persistFlushFind(Movie.builder()
                .title("The Godfather")
                .description("A crime dynasty story")
                .movieYear(1972)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build());

        review1 = em.persistFlushFind(Review.builder()
                .userId(10L)
                .movie(inception)
                .comment("Mind-blowing film!")
                .createdAt(LocalDateTime.now())
                .build());

        review2 = em.persistFlushFind(Review.builder()
                .userId(20L)
                .movie(inception)
                .comment("A masterpiece of cinema.")
                .createdAt(LocalDateTime.now())
                .build());

        review3 = em.persistFlushFind(Review.builder()
                .userId(10L)
                .movie(theGodfather)
                .comment("Classic Italian-American drama.")
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ── findReviewsByMovieId ──────────────────────────────────────────────────

    @Test
    void findReviewsByMovieId_returnsAllReviewsForMovie() {
        Page<Review> result = reviewRepository.findReviewsByMovieId(
                inception.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Review::getUserId)
                .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void findReviewsByMovieId_singleReview_returnsOne() {
        Page<Review> result = reviewRepository.findReviewsByMovieId(
                theGodfather.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("Classic Italian-American drama.");
    }

    @Test
    void findReviewsByMovieId_nonExistingMovie_returnsEmpty() {
        Page<Review> result = reviewRepository.findReviewsByMovieId(
                999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findReviewsByMovieId_pagination_limitsResults() {
        Page<Review> page1 = reviewRepository.findReviewsByMovieId(
                inception.getId(), PageRequest.of(0, 1));

        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getTotalElements()).isEqualTo(2);
    }

    // ── findReviewsByUserId ───────────────────────────────────────────────────

    @Test
    void findReviewsByUserId_userWithMultipleReviews_returnsAll() {
        Page<Review> result = reviewRepository.findReviewsByUserId(10L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(r -> r.getMovie().getId())
                .containsExactlyInAnyOrder(inception.getId(), theGodfather.getId());
    }

    @Test
    void findReviewsByUserId_userWithOneReview_returnsOne() {
        Page<Review> result = reviewRepository.findReviewsByUserId(20L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("A masterpiece of cinema.");
    }

    @Test
    void findReviewsByUserId_nonExistingUser_returnsEmpty() {
        Page<Review> result = reviewRepository.findReviewsByUserId(999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ── existsByUserIdAndMovieId ──────────────────────────────────────────────

    @Test
    void existsByUserIdAndMovieId_reviewExists_returnsTrue() {
        assertThat(reviewRepository.existsByUserIdAndMovieId(10L, inception.getId())).isTrue();
    }

    @Test
    void existsByUserIdAndMovieId_reviewDoesNotExist_returnsFalse() {
        assertThat(reviewRepository.existsByUserIdAndMovieId(99L, inception.getId())).isFalse();
    }

    @Test
    void existsByUserIdAndMovieId_sameUserDifferentMovie_returnsCorrectly() {
        assertThat(reviewRepository.existsByUserIdAndMovieId(10L, inception.getId())).isTrue();
        assertThat(reviewRepository.existsByUserIdAndMovieId(10L, theGodfather.getId())).isTrue();
        assertThat(reviewRepository.existsByUserIdAndMovieId(20L, theGodfather.getId())).isFalse();
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    void save_newReview_persistsSuccessfully() {
        Review newReview = Review.builder()
                .userId(30L)
                .movie(inception)
                .comment("Totally worth it!")
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(newReview);

        assertThat(saved.getId()).isNotNull();
        assertThat(reviewRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_existingReview_removesFromDatabase() {
        reviewRepository.delete(review1);
        em.flush();

        assertThat(reviewRepository.findById(review1.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllReviews() {
        assertThat(reviewRepository.findAll()).hasSize(3);
    }
}
