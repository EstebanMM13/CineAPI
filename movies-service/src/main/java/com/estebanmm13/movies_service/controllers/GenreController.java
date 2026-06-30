package com.estebanmm13.movies_service.controllers;

import com.estebanmm13.movies_service.dtoModels.response.GenreResponseDTO;

import com.estebanmm13.movies_service.error.dto.ResponseError;
import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.services.genre.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/genres")
@Tag(name = "Genres", description = "Genre management endpoints")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    // GET ALL GENRES
    @Operation(
            summary = "Find all genres",
            description = "Retrieve a paginated list of all available genres"
    )
    @ApiResponse(responseCode = "200", description = "Genres retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<GenreResponseDTO>> findAllGenre(
            @Parameter(description = "Pagination information") Pageable pageable) {

        return ResponseEntity.ok(genreService.findAllGenres(pageable));
    }


    // GET GENRE BY ID
    @Operation(summary = "Find genre by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Genre found",
                    content = @Content(schema = @Schema(implementation = Genre.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Genre not found",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponseDTO> findGenreById(
            @Parameter(description = "ID of the genre to retrieve", required = true)
            @PathVariable Long id) {

        return ResponseEntity.ok(genreService.findGenreById(id));
    }


    // CREATE GENRE
    @Operation(summary = "Create a new genre")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Genre created successfully",
                    content = @Content(schema = @Schema(implementation = Genre.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Genre> createGenre(
            @Parameter(description = "Genre object to create", required = true)
            @Valid @RequestBody Genre genre) {

        Genre createdGenre = genreService.createGenre(genre);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGenre);
    }


    // UPDATE GENRE
    @Operation(summary = "Update an existing genre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Genre updated successfully"),
            @ApiResponse(responseCode = "404", description = "Genre not found"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Genre> updateGenre(
            @Parameter(description = "ID of the genre to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Genre fields to update", required = true)
            @Valid @RequestBody Genre genre) {

        return ResponseEntity.ok(genreService.updateGenre(id, genre));
    }


    // DELETE GENRE
    @Operation(summary = "Delete a genre by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Genre deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Genre not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGenre(
            @Parameter(description = "ID of the genre to delete", required = true)
            @PathVariable Long id) {

        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }

    // SEARCH GENRE BY NAME
    @Operation(
            summary = "Find genres by name",
            description = "Retrieve a paginated list of genres filtered by name"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Genres found"),
            @ApiResponse(responseCode = "400", description = "Invalid name parameter")
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<Page<GenreResponseDTO>> findGenreByName(
            @Parameter(description = "Name or partial name to search", required = true)
            @PathVariable String name,
            Pageable pageable) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name parameter is required");
        }

        return ResponseEntity.ok(
                genreService.findGenreByName(name, pageable)
        );
    }
}