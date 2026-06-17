package com.estebanmm13.movies_service.error.notFound;

public class DuplicateVoteException extends RuntimeException {

    public static final String ALREADY_VOTED = "User with ID %d has already voted for movie with ID %d";

    public DuplicateVoteException(String message) {
        super(message);
    }
}
