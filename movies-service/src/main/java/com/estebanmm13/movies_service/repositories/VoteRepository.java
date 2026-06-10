package com.estebanmm13.movies_service.repositories;


import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Vote;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
}

