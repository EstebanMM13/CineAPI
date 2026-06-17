package com.estebanmm13.movies_service.error.notFound;

public class DuplicateReviewException extends RuntimeException {

    public static final String ALREADY_REVIEWED = "User with ID %d already submitted a review for movie with ID %d";

    public DuplicateReviewException(String message) {
        super(message);
    }
}
