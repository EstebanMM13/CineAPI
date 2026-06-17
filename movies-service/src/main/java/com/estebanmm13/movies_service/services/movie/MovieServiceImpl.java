package com.estebanmm13.movies_service.services.movie;


import com.estebanmm13.movies_service.dtoModels.request.MovieRequestDTO;
import com.estebanmm13.movies_service.dtoModels.response.MovieResponseDTO;
import com.estebanmm13.movies_service.error.notFound.DuplicateVoteException;
import com.estebanmm13.movies_service.error.notFound.MovieNotFoundException;
import com.estebanmm13.movies_service.mapper.MovieMapper;
import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.models.Movie;
import com.estebanmm13.movies_service.models.Vote;
import com.estebanmm13.movies_service.repositories.GenreRepository;
import com.estebanmm13.movies_service.repositories.MovieRepository;
import com.estebanmm13.movies_service.repositories.VoteRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final VoteRepository voteRepository;
    private final MovieMapper movieMapper;

    // Constructor injection
    public MovieServiceImpl(MovieRepository movieRepository,
                            GenreRepository genreRepository,
                            VoteRepository voteRepository,
                            MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.voteRepository = voteRepository;
        this.movieMapper = movieMapper;
    }

    @Override
    public Page<MovieResponseDTO> findAllMovies(Pageable pageable) {
        log.info("Petición para listar todas las películas");
        return movieRepository.findAll(pageable)
                .map(movieMapper::toResponseDTO);
    }

    @Override
    public MovieResponseDTO findMovieById(Long id) {
        log.debug("Finding movie by id: {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
        return movieMapper.toResponseDTO(movie);
    }

    @Override
    public MovieResponseDTO createMovie(MovieRequestDTO dto) {
        log.info("Creating new movie with title: {}", dto.getTitle());
        Movie movie = movieMapper.toEntity(dto);
        // Asignar géneros si vienen IDs
        if (dto.getGenreIds() != null && !dto.getGenreIds().isEmpty()) {
            List<Genre> genres = genreRepository.findAllById(dto.getGenreIds());
            movie.setGenres(genres);
        } else {
            movie.setGenres(new ArrayList<>());
        }
        Movie saved = movieRepository.save(movie);
        log.info("Movie created with id: {}", saved.getId());
        return movieMapper.toResponseDTO(saved);
    }

    @Override
    public MovieResponseDTO updateMovie(Long id, MovieRequestDTO dto) {
        Movie existing = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
        movieMapper.updateEntity(existing, dto);
        // Actualizar géneros si se proporcionan
        if (dto.getGenreIds() != null) {
            List<Genre> genres = genreRepository.findAllById(dto.getGenreIds());
            existing.setGenres(genres);
        }
        Movie updated = movieRepository.save(existing);
        return movieMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteMovie(Long id) {
        log.warn("Attempt to delete non-existent movie with id: {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + id));
        movieRepository.delete(movie);
        log.info("Movie deleted with id: {}", id);
    }

    @Override
    public MovieResponseDTO voteMovie(Long movieId, Long userId, Double rating) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with id: " + movieId));

        if (voteRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new DuplicateVoteException(String.format(DuplicateVoteException.ALREADY_VOTED, userId, movieId));
        }

        Vote vote = Vote.builder()
                .movie(movie)
                .userId(userId)
                .rating(rating)
                .votedAt(LocalDateTime.now())
                .build();
        voteRepository.save(vote);

        // Actualizar rating promedio y número de votos
        double totalRating = movie.getRating() * movie.getVotes() + rating;
        int totalVotes = movie.getVotes() + 1;
        movie.setVotes(totalVotes);
        movie.setRating(totalRating / totalVotes);

        Movie updatedMovie = movieRepository.save(movie);
        return movieMapper.toResponseDTO(updatedMovie);
    }

    @Override
    public Page<MovieResponseDTO> findMovieByTitleContaining(String title, Pageable pageable) {
        return movieRepository.findMovieByTitleContaining(title, pageable)
                .map(movieMapper::toResponseDTO);
    }

    @Override
    public Page<MovieResponseDTO> findAllMoviesByGenre(String name, Pageable pageable) {
        return movieRepository.findAllByGenreName(name, pageable)
                .map(movieMapper::toResponseDTO);
    }
}