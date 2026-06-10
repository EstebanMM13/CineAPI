package com.estebanmm13.movies_service.mapper;


import com.estebanmm13.movies_service.dtoModels.request.ReviewRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Review;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReviewMapper {

    public ReviewResponseDTO toResponseDTO(Review review) {
        if (review == null) return null;
        return new ReviewResponseDTO(
                review.getId(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUserId(),
                review.getMovie().getTitle()
        );
    }

    public Review toEntity(ReviewRequestDTO dto, Long userId, Movie movie) {
        if (dto == null) return null;
        return Review.builder()
                .comment(dto.getComment())
                .userId(userId)
                .movie(movie)
                .createdAt(LocalDateTime.now())
                .build();
    }
}