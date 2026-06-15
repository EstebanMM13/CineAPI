package com.estebanmm13.movies_service.services.movie;

import com.estebanmm13.movies_service.dtoModels.request.MovieRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.GenreResponseDTO;
import com.estebanmm13.movies_service.dtoModels.response.MovieResponseDTO;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.mapper.MovieMapper;
import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Vote;
import com.estebanmm13.movies_service.repositories.GenreRepository;
import com.estebanmm13.movies_service.repositories.MovieRepository;
import com.estebanmm13.movies_service.repositories.VoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock private MovieRepository movieRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private MovieMapper movieMapper;

    @InjectMocks private MovieServiceImpl movieService;

    private Movie movie(Long id) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle("Movie " + id);
        m.setMovieYear(2020);
        m.setVotes(0);
        m.setRating(0.0);
        m.setGenres(List.of());
        return m;
    }

    private MovieResponseDTO movieDto(Long id) {
        return new MovieResponseDTO(id, "Movie " + id, "desc", 2020, 0, 0.0, null, List.of());
    }

    private MovieRequestDTO movieRequest() {
        MovieRequestDTO dto = new MovieRequestDTO();
        dto.setTitle("Inception");
        dto.setDescription("A mind-bending thriller");
        dto.setMovieYear(2010);
        dto.setGenreIds(List.of());
        return dto;
    }

    // ── findAllMovies ─────────────────────────────────────────────────────────

    @Test
    void findAllMovies_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie m = movie(1L);
        Page<Movie> page = new PageImpl<>(List.of(m));

        given(movieRepository.findAll(pageable)).willReturn(page);
        given(movieMapper.toResponseDTO(m)).willReturn(movieDto(1L));

        Page<MovieResponseDTO> result = movieService.findAllMovies(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    // ── findMovieById ─────────────────────────────────────────────────────────

    @Test
    void findMovieById_existingId_returnsDTO() {
        Movie m = movie(1L);
        MovieResponseDTO expected = movieDto(1L);

        given(movieRepository.findById(1L)).willReturn(Optional.of(m));
        given(movieMapper.toResponseDTO(m)).willReturn(expected);

        MovieResponseDTO result = movieService.findMovieById(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findMovieById_notFound_throwsMovieNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.findMovieById(99L))
                .isInstanceOf(MovieNotFoundException.class);
    }

    // ── createMovie ───────────────────────────────────────────────────────────

    @Test
    void createMovie_withoutGenres_savesAndReturnsDTO() {
        MovieRequestDTO req = movieRequest();
        Movie entity = movie(1L);
        MovieResponseDTO expected = movieDto(1L);

        given(movieMapper.toEntity(req)).willReturn(entity);
        given(movieRepository.save(entity)).willReturn(entity);
        given(movieMapper.toResponseDTO(entity)).willReturn(expected);

        MovieResponseDTO result = movieService.createMovie(req);

        assertThat(result).isEqualTo(expected);
        then(movieRepository).should().save(entity);
    }

    @Test
    void createMovie_withGenreIds_associatesGenres() {
        MovieRequestDTO req = movieRequest();
        req.setGenreIds(List.of(1L, 2L));
        Genre g1 = Genre.builder().id(1L).name("Action").build();
        Genre g2 = Genre.builder().id(2L).name("Thriller").build();
        Movie entity = movie(1L);
        MovieResponseDTO expected = movieDto(1L);

        given(movieMapper.toEntity(req)).willReturn(entity);
        given(genreRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(g1, g2));
        given(movieRepository.save(entity)).willReturn(entity);
        given(movieMapper.toResponseDTO(entity)).willReturn(expected);

        movieService.createMovie(req);

        assertThat(entity.getGenres()).containsExactly(g1, g2);
    }

    // ── updateMovie ───────────────────────────────────────────────────────────

    @Test
    void updateMovie_existingId_updatesAndReturnsDTO() {
        Movie existing = movie(1L);
        MovieRequestDTO req = movieRequest();
        MovieResponseDTO expected = movieDto(1L);

        given(movieRepository.findById(1L)).willReturn(Optional.of(existing));
        given(movieRepository.save(existing)).willReturn(existing);
        given(movieMapper.toResponseDTO(existing)).willReturn(expected);

        MovieResponseDTO result = movieService.updateMovie(1L, req);

        assertThat(result).isEqualTo(expected);
        then(movieMapper).should().updateEntity(existing, req);
    }

    @Test
    void updateMovie_notFound_throwsMovieNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.updateMovie(99L, movieRequest()))
                .isInstanceOf(MovieNotFoundException.class);
    }

    // ── deleteMovie ───────────────────────────────────────────────────────────

    @Test
    void deleteMovie_existingId_callsDelete() {
        Movie m = movie(1L);
        given(movieRepository.findById(1L)).willReturn(Optional.of(m));

        movieService.deleteMovie(1L);

        then(movieRepository).should().delete(m);
    }

    @Test
    void deleteMovie_notFound_throwsMovieNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.deleteMovie(99L))
                .isInstanceOf(MovieNotFoundException.class);

        then(movieRepository).should(never()).delete(any());
    }

    // ── voteMovie ─────────────────────────────────────────────────────────────

    @Test
    void voteMovie_firstVote_updatesRatingCorrectly() {
        Movie m = movie(1L);
        m.setVotes(0);
        m.setRating(0.0);
        MovieResponseDTO expected = new MovieResponseDTO(1L, "Movie 1", "desc", 2020, 1, 8.0, null, List.of());

        given(movieRepository.findById(1L)).willReturn(Optional.of(m));
        given(movieRepository.save(any(Movie.class))).willReturn(m);
        given(movieMapper.toResponseDTO(any(Movie.class))).willReturn(expected);

        movieService.voteMovie(1L, 10L, 8.0);

        assertThat(m.getVotes()).isEqualTo(1);
        assertThat(m.getRating()).isEqualTo(8.0);
        then(voteRepository).should().save(any(Vote.class));
    }

    @Test
    void voteMovie_secondVote_averagesRating() {
        Movie m = movie(1L);
        m.setVotes(1);
        m.setRating(8.0);

        given(movieRepository.findById(1L)).willReturn(Optional.of(m));
        given(movieRepository.save(any(Movie.class))).willReturn(m);
        given(movieMapper.toResponseDTO(any())).willReturn(movieDto(1L));

        movieService.voteMovie(1L, 11L, 6.0);

        assertThat(m.getVotes()).isEqualTo(2);
        assertThat(m.getRating()).isEqualTo(7.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void voteMovie_movieNotFound_throwsMovieNotFoundException() {
        given(movieRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.voteMovie(99L, 1L, 7.0))
                .isInstanceOf(MovieNotFoundException.class);
    }

    // ── findMovieByTitleContaining ────────────────────────────────────────────

    @Test
    void findMovieByTitleContaining_returnsMatchingPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie m = movie(1L);
        Page<Movie> page = new PageImpl<>(List.of(m));

        given(movieRepository.findMovieByTitleContaining("Inception", pageable)).willReturn(page);
        given(movieMapper.toResponseDTO(m)).willReturn(movieDto(1L));

        Page<MovieResponseDTO> result = movieService.findMovieByTitleContaining("Inception", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── findAllMoviesByGenre ──────────────────────────────────────────────────

    @Test
    void findAllMoviesByGenre_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Movie m = movie(1L);
        Page<Movie> page = new PageImpl<>(List.of(m));

        given(movieRepository.findAllByGenreName("Action", pageable)).willReturn(page);
        given(movieMapper.toResponseDTO(m)).willReturn(movieDto(1L));

        Page<MovieResponseDTO> result = movieService.findAllMoviesByGenre("Action", pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
