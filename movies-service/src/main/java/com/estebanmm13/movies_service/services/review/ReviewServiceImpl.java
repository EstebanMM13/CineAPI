package com.estebanmm13.movies_service.services.review;


import com.estebanmm13.movies_service.clients.UsernameResolver;
import com.estebanmm13.movies_service.dtoModels.request.ReviewRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.error.notFound.ReviewNotFoundException;
import com.estebanmm13.movies_service.mapper.ReviewMapper;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Review;
import com.estebanmm13.movies_service.repositories.MovieRepository;
import com.estebanmm13.movies_service.repositories.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.estebanmm13.movies_service.error.notFound.DuplicateReviewException;
import com.estebanmm13.movies_service.error.notFound.UnauthorizedActionException;


@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final ReviewMapper reviewMapper;
    private final UsernameResolver usernameResolver;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             MovieRepository movieRepository,
                             ReviewMapper reviewMapper,
                             UsernameResolver usernameResolver) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.reviewMapper = reviewMapper;
        this.usernameResolver = usernameResolver;
    }

    private ReviewResponseDTO toResponseDTOWithUsername(Review review) {
        ReviewResponseDTO dto = reviewMapper.toResponseDTO(review);
        dto.setUsername(usernameResolver.resolveUsername(review.getUserId()));
        return dto;
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

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(String.format(MovieNotFoundException.NOT_FOUND_BY_ID, movieId)));

        if (reviewRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new DuplicateReviewException(String.format(DuplicateReviewException.ALREADY_REVIEWED, userId, movieId));
        }

        Review review = reviewMapper.toEntity(dto, userId, movie);
        Review saved = reviewRepository.save(review);
        return toResponseDTOWithUsername(saved);
    }

    @Override
    public ReviewResponseDTO updateReview(Long id, Long userId, ReviewRequestDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(String.format(ReviewNotFoundException.NOT_FOUND_BY_ID, id)));

        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedActionException(UnauthorizedActionException.NOT_YOUR_REVIEW);
        }

        review.setComment(dto.getComment());
        Review updated = reviewRepository.save(review);
        return toResponseDTOWithUsername(updated);
    }


    @Override
    public void deleteReview(Long reviewId, Long userId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(String.format(ReviewNotFoundException.NOT_FOUND_BY_ID, reviewId)));

        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedActionException(UnauthorizedActionException.NOT_YOUR_REVIEW);
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