package com.estebanmm13.movies_service.services.review;


import com.estebanmm13.movies_service.clients.AuthServiceClient;
import com.estebanmm13.movies_service.dtoModels.request.ReviewRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.error.notFound.ReviewNotFoundException;
import com.estebanmm13.movies_service.mapper.ReviewMapper;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Review;
import com.estebanmm13.movies_service.repositories.MovieRepository;
import com.estebanmm13.movies_service.repositories.ReviewRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.estebanmm13.movies_service.error.notFound.ReviewNotFoundException.NOT_ACCES;


@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String UNKNOWN_USERNAME = "Usuario desconocido";

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final ReviewMapper reviewMapper;
    private final AuthServiceClient authServiceClient;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             MovieRepository movieRepository,
                             ReviewMapper reviewMapper,
                             AuthServiceClient authServiceClient) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.reviewMapper = reviewMapper;
        this.authServiceClient = authServiceClient;
    }

    private ReviewResponseDTO toResponseDTOWithUsername(Review review) {
        ReviewResponseDTO dto = reviewMapper.toResponseDTO(review);
        dto.setUsername(resolveUsername(review.getUserId()));
        return dto;
    }

    private String resolveUsername(Long userId) {
        try {
            return authServiceClient.getUsernameById(userId);
        } catch (FeignException ex) {
            log.warn("No se pudo resolver el username del usuario {} en auth-service: {}", userId, ex.getMessage());
            return UNKNOWN_USERNAME;
        }
    }

    @Override
    public List<ReviewResponseDTO> findAllReviews() {
        return reviewRepository.findAll()
                .stream().map(this::toResponseDTOWithUsername)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponseDTO findReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(String.format(ReviewNotFoundException.NOT_FOUND_BY_ID, id)));
        return toResponseDTOWithUsername(review);
    }

    @Override
    public ReviewResponseDTO createReview(Long userId, Long movieId, ReviewRequestDTO dto) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UserNotFoundException(String.format(UserNotFoundException.NOT_FOUND_BY_ID, userId)));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(String.format(MovieNotFoundException.NOT_FOUND_BY_ID, movieId)));

        if (reviewRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new RuntimeException("User already submitted a review for this movie");
        }

        Review review = reviewMapper.toEntity(dto, userId, movie);
        Review saved = reviewRepository.save(review);
        return toResponseDTOWithUsername(saved);
    }

    @Override
    public ReviewResponseDTO updateReview(Long id, Long userId, ReviewRequestDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(String.format(ReviewNotFoundException.NOT_FOUND_BY_ID, id)));

        // ✅ Ahora funciona porque review.getUserId() es un Long directamente
        if (!review.getUserId().equals(userId)) {
            throw new ReviewNotFoundException(NOT_ACCES);
        }

        review.setComment(dto.getComment());
        Review updated = reviewRepository.save(review);
        return toResponseDTOWithUsername(updated);
    }


    @Override
    public void deleteReview(Long reviewId, Long userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(String.format(ReviewNotFoundException.NOT_FOUND_BY_ID, reviewId)));

        // ✅ Ahora funciona porque review.getUserId() es un Long directamente
        if (!review.getUserId().equals(userId)) {
            throw new ReviewNotFoundException(NOT_ACCES);
        }

        reviewRepository.deleteById(reviewId);
    }

    @Override
    public Page<ReviewResponseDTO> findReviewsByMovieId(Long movieId, Pageable pageable) {
        return reviewRepository.findReviewsByMovieId(movieId, pageable).map(this::toResponseDTOWithUsername);
    }

    @Override
    public Page<ReviewResponseDTO> findReviewsByUserId(Long userId, Pageable pageable) {
        return reviewRepository.findReviewsByUserId(userId, pageable).map(this::toResponseDTOWithUsername);
    }
}