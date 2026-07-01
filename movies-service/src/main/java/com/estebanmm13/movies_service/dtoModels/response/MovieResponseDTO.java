package com.estebanmm13.movies_service.dtoModels.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "Movie information returned to the client")
public class MovieResponseDTO implements Serializable {
    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @Schema(description = "Title of the movie", example = "Inception")
    private String title;

    @Schema(description = "Detailed description", example = "A thief who steals corporate secrets...")
    private String description;

    @Schema(description = "Release year", example = "2010")
    private int movieYear;

    @Schema(description = "Number of votes", example = "1234")
    private int votes;

    @Schema(description = "Average rating (0-10)", example = "8.8")
    private double rating;

    @Schema(description = "URL of the poster image", example = "https://example.com/inception.jpg")
    private String imageUrl;

    @Schema(description = "List of genres")
    private List<GenreResponseDTO> genres;

    // Constructor con todos los campos (para facilitar la creación)
    public MovieResponseDTO(Long id, String title, String description, int movieYear,
                            int votes, double rating, String imageUrl, List<GenreResponseDTO> genres) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.movieYear = movieYear;
        this.votes = votes;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.genres = genres;
    }
}