package com.estebanmm13.movies_service.controllers;


import com.estebanmm13.movies_service.config.UserPrincipal;
import com.estebanmm13.movies_service.dtoModels.request.MovieRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.MovieResponseDTO;
import com.estebanmm13.movies_service.services.movie.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.estebanmm13.movies_service.error.dto.ResponseError;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movies", description = "Movie management endpoints")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @Operation(summary = "Find all movies", description = "Retrieve a paginated list of all movies")
    @ApiResponse(responseCode = "200", description = "Movies retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<MovieResponseDTO>> findAllMovies(
            @Parameter(description = "Pagination information") Pageable pageable) {
        return ResponseEntity.ok(movieService.findAllMovies(pageable));
    }

    @Operation(summary = "Find movie by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie found",
                    content = @Content(schema = @Schema(implementation = MovieResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> findMovieById(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(movieService.findMovieById(id));
    }

    @Operation(summary = "Create a new movie", description = "Creates a new movie. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movie created successfully",
                    content = @Content(schema = @Schema(implementation = MovieResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDTO> createMovie(@Valid @RequestBody MovieRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(dto));
    }

    @Operation(summary = "Update an existing movie", description = "Full replacement of movie data. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie updated successfully",
                    content = @Content(schema = @Schema(implementation = MovieResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDTO> updateMovie(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long id,
            @Valid @RequestBody MovieRequestDTO dto) {
        return ResponseEntity.ok(movieService.updateMovie(id, dto));
    }

    @Operation(summary = "Delete a movie by ID", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Movie deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMovie(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find movies by title", description = "Case-insensitive partial match on title")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movies found"),
            @ApiResponse(responseCode = "400", description = "Title parameter is blank",
                    content = @Content(schema = @Schema(implementation = ResponseError.class)))
    })
    @GetMapping("/title")
    public ResponseEntity<Page<MovieResponseDTO>> findMovieByTitle(
            @Parameter(description = "Title or partial title to search", required = true) @RequestParam String title,
            @Parameter(description = "Pagination information") Pageable pageable) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title parameter is required");
        }
        return ResponseEntity.ok(movieService.findMovieByTitleContaining(title, pageable));
    }

    @Operation(summary = "Vote a movie", description = "Rate a movie from 1.0 to 10.0. Each user can only vote once per movie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vote registered successfully",
                    content = @Content(schema = @Schema(implementation = MovieResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Rating out of range",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class)))
    })
    @PutMapping("/{movieId}/vote/{rating}")
    public ResponseEntity<MovieResponseDTO> voteMovie(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long movieId,
            @Parameter(description = "Rating value between 1.0 and 10.0", required = true) @PathVariable Double rating,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.userId();
        if (rating < 1.0 || rating > 10.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 10.0");
        }
        return ResponseEntity.ok(movieService.voteMovie(movieId, userId, rating));
    }

    @Operation(summary = "Find movies by genre", description = "Filter paginated movies by genre name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movies found"),
            @ApiResponse(responseCode = "404", description = "Genre not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class)))
    })
    @GetMapping("/genre/{name}")
    public ResponseEntity<Page<MovieResponseDTO>> findAllMoviesByGenre(
            @Parameter(description = "Genre name", required = true) @PathVariable String name,
            @Parameter(description = "Pagination information") Pageable pageable) {
        return ResponseEntity.ok(movieService.findAllMoviesByGenre(name, pageable));
    }
}