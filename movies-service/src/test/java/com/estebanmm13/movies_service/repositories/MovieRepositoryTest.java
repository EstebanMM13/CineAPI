package com.estebanmm13.movies_service.repositories;

import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.models.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MovieRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private MovieRepository movieRepository;

    private Genre action;
    private Genre drama;
    private Movie inception;
    private Movie theGodfather;
    private Movie interstellar;

    @BeforeEach
    void setUp() {
        action = em.persistFlushFind(Genre.builder().name("Action").build());
        drama = em.persistFlushFind(Genre.builder().name("Drama").build());

        inception = em.persistFlushFind(Movie.builder()
                .title("Inception")
                .description("A thief who steals corporate secrets")
                .movieYear(2010)
                .votes(0)
                .rating(0.0)
                .genres(List.of(action))
                .build());

        theGodfather = em.persistFlushFind(Movie.builder()
                .title("The Godfather")
                .description("The aging patriarch of an organized crime dynasty")
                .movieYear(1972)
                .votes(0)
                .rating(0.0)
                .genres(List.of(drama))
                .build());

        interstellar = em.persistFlushFind(Movie.builder()
                .title("Interstellar")
                .description("A team of explorers travel through a wormhole")
                .movieYear(2014)
                .votes(0)
                .rating(0.0)
                .genres(List.of(action, drama))
                .build());
    }

    // ── findMovieByTitleContaining ────────────────────────────────────────────

    @Test
    void findMovieByTitleContaining_exactTitle_returnsMatch() {
        Page<Movie> result = movieRepository.findMovieByTitleContaining(
                "Inception", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Inception");
    }

    @Test
    void findMovieByTitleContaining_partialTitle_returnsMatches() {
        Page<Movie> result = movieRepository.findMovieByTitleContaining(
                "Inter", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Interstellar");
    }

    @Test
    void findMovieByTitleContaining_commonSubstring_returnsMultiple() {
        Page<Movie> result = movieRepository.findMovieByTitleContaining(
                "the", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Godfather");
    }

    @Test
    void findMovieByTitleContaining_noMatch_returnsEmpty() {
        Page<Movie> result = movieRepository.findMovieByTitleContaining(
                "xyz999", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findMovieByTitleContaining_pagination_limitsResults() {
        Page<Movie> result = movieRepository.findMovieByTitleContaining(
                "", PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // ── findAllByGenreName ────────────────────────────────────────────────────

    @Test
    void findAllByGenreName_exactGenre_returnsMoviesInGenre() {
        Page<Movie> result = movieRepository.findAllByGenreName(
                "Drama", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Movie::getTitle)
                .containsExactlyInAnyOrder("The Godfather", "Interstellar");
    }

    @Test
    void findAllByGenreName_caseInsensitive_returnsMovies() {
        Page<Movie> result = movieRepository.findAllByGenreName(
                "ACTION", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Movie::getTitle)
                .containsExactlyInAnyOrder("Inception", "Interstellar");
    }

    @Test
    void findAllByGenreName_nonExistingGenre_returnsEmpty() {
        Page<Movie> result = movieRepository.findAllByGenreName(
                "Horror", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllByGenreName_movieWithMultipleGenres_appearsForEachGenre() {
        Page<Movie> actionResult = movieRepository.findAllByGenreName(
                "Action", PageRequest.of(0, 10));
        Page<Movie> dramaResult = movieRepository.findAllByGenreName(
                "Drama", PageRequest.of(0, 10));

        boolean interstellarInAction = actionResult.getContent().stream()
                .anyMatch(m -> m.getTitle().equals("Interstellar"));
        boolean interstellarInDrama = dramaResult.getContent().stream()
                .anyMatch(m -> m.getTitle().equals("Interstellar"));

        assertThat(interstellarInAction).isTrue();
        assertThat(interstellarInDrama).isTrue();
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    void save_newMovie_persistsSuccessfully() {
        Movie newMovie = Movie.builder()
                .title("Pulp Fiction")
                .description("The lives of two mob hitmen")
                .movieYear(1994)
                .votes(0)
                .rating(0.0)
                .genres(List.of())
                .build();

        Movie saved = movieRepository.save(newMovie);

        assertThat(saved.getId()).isNotNull();
        assertThat(movieRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_existingMovie_removesFromDatabase() {
        movieRepository.delete(inception);
        em.flush();

        assertThat(movieRepository.findById(inception.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllMovies() {
        assertThat(movieRepository.findAll()).hasSize(3);
    }
}
