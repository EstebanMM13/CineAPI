package com.estebanmm13.movies_service.repositories;

import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class VoteRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private VoteRepository voteRepository;

    private Movie inception;
    private Movie theGodfather;
    private Vote vote1;
    private Vote vote2;

    @BeforeEach
    void setUp() {
        inception = em.persistFlushFind(Movie.builder()
                .title("Inception")
                .description("A thief who steals corporate secrets")
                .movieYear(2010)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build());

        theGodfather = em.persistFlushFind(Movie.builder()
                .title("The Godfather")
                .description("A crime dynasty story")
                .movieYear(1972)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build());

        vote1 = em.persistFlushFind(Vote.builder()
                .movie(inception)
                .userId(10L)
                .rating(8.5)
                .votedAt(LocalDateTime.now())
                .build());

        vote2 = em.persistFlushFind(Vote.builder()
                .movie(theGodfather)
                .userId(10L)
                .rating(9.0)
                .votedAt(LocalDateTime.now())
                .build());
    }

    // ── existsByUserIdAndMovieId ──────────────────────────────────────────────

    @Test
    void existsByUserIdAndMovieId_voteExists_returnsTrue() {
        assertThat(voteRepository.existsByUserIdAndMovieId(10L, inception.getId())).isTrue();
    }

    @Test
    void existsByUserIdAndMovieId_voteDoesNotExist_returnsFalse() {
        assertThat(voteRepository.existsByUserIdAndMovieId(99L, inception.getId())).isFalse();
    }

    @Test
    void existsByUserIdAndMovieId_sameUserDifferentMovies_returnsCorrectly() {
        assertThat(voteRepository.existsByUserIdAndMovieId(10L, inception.getId())).isTrue();
        assertThat(voteRepository.existsByUserIdAndMovieId(10L, theGodfather.getId())).isTrue();
    }

    @Test
    void existsByUserIdAndMovieId_differentUserSameMovie_returnsFalse() {
        assertThat(voteRepository.existsByUserIdAndMovieId(20L, inception.getId())).isFalse();
    }

    // ── UniqueConstraint enforcement ──────────────────────────────────────────

    @Test
    void save_duplicateUserMovieCombination_throwsException() {
        Vote duplicate = Vote.builder()
                .movie(inception)
                .userId(10L)
                .rating(7.0)
                .votedAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> {
            voteRepository.saveAndFlush(duplicate);
        }).isInstanceOf(Exception.class);
    }

    @Test
    void save_sameUserDifferentMovie_succeeds() {
        Movie newMovie = em.persistFlushFind(Movie.builder()
                .title("Interstellar")
                .description("A space exploration story")
                .movieYear(2014)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build());

        Vote newVote = Vote.builder()
                .movie(newMovie)
                .userId(10L)
                .rating(8.0)
                .votedAt(LocalDateTime.now())
                .build();

        Vote saved = voteRepository.saveAndFlush(newVote);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_differentUserSameMovie_succeeds() {
        Vote newVote = Vote.builder()
                .movie(inception)
                .userId(20L)
                .rating(7.5)
                .votedAt(LocalDateTime.now())
                .build();

        Vote saved = voteRepository.saveAndFlush(newVote);

        assertThat(saved.getId()).isNotNull();
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    void save_newVote_persistsSuccessfully() {
        Vote newVote = Vote.builder()
                .movie(inception)
                .userId(30L)
                .rating(6.5)
                .votedAt(LocalDateTime.now())
                .build();

        Vote saved = voteRepository.save(newVote);

        assertThat(saved.getId()).isNotNull();
        assertThat(voteRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_existingVote_removesFromDatabase() {
        voteRepository.delete(vote1);
        em.flush();

        assertThat(voteRepository.findById(vote1.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllVotes() {
        assertThat(voteRepository.findAll()).hasSize(2);
    }

    @Test
    void findById_existingVote_returnsVoteWithRating() {
        Vote found = voteRepository.findById(vote1.getId()).orElseThrow();

        assertThat(found.getRating()).isEqualTo(8.5);
        assertThat(found.getUserId()).isEqualTo(10L);
    }
}
