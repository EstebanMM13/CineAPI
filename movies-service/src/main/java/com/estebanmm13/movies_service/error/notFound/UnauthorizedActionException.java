package com.estebanmm13.movies_service.error.notFound;

public class UnauthorizedActionException extends RuntimeException {

    public static final String NOT_YOUR_REVIEW = "You are not authorized to modify or delete this review";

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
