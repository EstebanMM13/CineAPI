package com.estebanmm13.movies_service.dtoModels.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Review information returned to the client")
public class ReviewResponseDTO {
    @Schema(description = "Review ID", example = "10")
    private Long id;

    @Schema(description = "Review comment", example = "This movie is amazing!")
    private String comment;

    @JsonFormat(pattern = "dd-MM-yyyy 'at' HH:mm")
    @Schema(description = "Date and time of creation", example = "30-04-2025 at 12:34:56")
    private LocalDateTime createdAt;

    private Long userId;

    @Schema(description = "Username of the review author, resolved from auth-service", example = "johndoe")
    private String username;

    @Schema(description = "Title of the reviewed movie", example = "Inception")
    private String movieTitle;
}

