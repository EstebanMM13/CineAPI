package com.estebanmm13.movies_service.controllers;


import com.estebanmm13.movies_service.dtoModels.response.ReviewResponseDTO;
import com.estebanmm13.movies_service.error.dto.ResponseError;
import com.estebanmm13.movies_service.mapper.ReviewMapper;
import com.estebanmm13.movies_service.services.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Review User REST Controller
 * Handles retrieval of reviews made by a specific user.
 *
 * Endpoint: /api/reviews/{userId}
 *
 * @author Esteban
 */
@RestController
@RequestMapping("/api/reviews/{userId}")
@Tag(name = "User Reviews", description = "User review retrieval endpoints")
public class ReviewUsersController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    public ReviewUsersController(ReviewService reviewService,
                                 ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
    }

    // GET REVIEWS BY USER
    @Operation(
            summary = "Get reviews by user",
            description = "Retrieve a paginated list of all reviews made by a specific user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> findReviewsByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Pagination information")
            Pageable pageable) {

        return ResponseEntity.ok(reviewService.findReviewsByUserId(userId, pageable));
    }
}