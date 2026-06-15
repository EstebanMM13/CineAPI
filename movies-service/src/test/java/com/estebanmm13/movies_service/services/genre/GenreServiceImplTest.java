package com.estebanmm13.movies_service.services.genre;

import com.estebanmm13.movies_service.dtoModels.response.GenreResponseDTO;
import com.estebanmm13.movies_service.error.notFound.GenreNotFoundException;
import com.estebanmm13.movies_service.mapper.GenreMapper;
import com.estebanmm13.movies_service.models.Genre;
import com.estebanmm13.movies_service.repositories.GenreRepository;
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
class GenreServiceImplTest {

    @Mock private GenreRepository genreRepository;
    @Mock private GenreMapper genreMapper;

    @InjectMocks private GenreServiceImpl genreService;

    private Genre genre(Long id, String name) {
        return Genre.builder().id(id).name(name).build();
    }

    private GenreResponseDTO dto(Long id, String name) {
        return new GenreResponseDTO(id, name);
    }

    // ── findAllGenres ─────────────────────────────────────────────────────────

    @Test
    void findAllGenres_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Genre g = genre(1L, "Action");
        Page<Genre> page = new PageImpl<>(List.of(g));

        given(genreRepository.findAll(pageable)).willReturn(page);
        given(genreMapper.toResponseDTO(g)).willReturn(dto(1L, "Action"));

        Page<GenreResponseDTO> result = genreService.findAllGenres(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Action");
    }

    // ── findGenreById ─────────────────────────────────────────────────────────

    @Test
    void findGenreById_existingId_returnsDTO() {
        Genre g = genre(1L, "Action");
        given(genreRepository.findById(1L)).willReturn(Optional.of(g));
        given(genreMapper.toResponseDTO(g)).willReturn(dto(1L, "Action"));

        GenreResponseDTO result = genreService.findGenreById(1L);

        assertThat(result.getName()).isEqualTo("Action");
    }

    @Test
    void findGenreById_notFound_throwsGenreNotFoundException() {
        given(genreRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.findGenreById(99L))
                .isInstanceOf(GenreNotFoundException.class);
    }

    // ── createGenre ───────────────────────────────────────────────────────────

    @Test
    void createGenre_newName_savesAndReturns() {
        Genre g = genre(null, "Drama");
        Genre saved = genre(1L, "Drama");

        given(genreRepository.existsByNameIgnoreCase("Drama")).willReturn(false);
        given(genreRepository.save(g)).willReturn(saved);

        Genre result = genreService.createGenre(g);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Drama");
    }

    @Test
    void createGenre_duplicateName_throwsIllegalArgumentException() {
        Genre g = genre(null, "Action");
        given(genreRepository.existsByNameIgnoreCase("Action")).willReturn(true);

        assertThatThrownBy(() -> genreService.createGenre(g))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Action");

        then(genreRepository).should(never()).save(any());
    }

    // ── updateGenre ───────────────────────────────────────────────────────────

    @Test
    void updateGenre_existingIdNewName_updatesSuccessfully() {
        Genre existing = genre(1L, "Action");
        Genre updated = genre(1L, "Thriller");

        given(genreRepository.findById(1L)).willReturn(Optional.of(existing));
        given(genreRepository.existsByNameIgnoreCase("Thriller")).willReturn(false);
        given(genreRepository.save(any(Genre.class))).willReturn(updated);

        Genre result = genreService.updateGenre(1L, genre(null, "Thriller"));

        assertThat(result.getName()).isEqualTo("Thriller");
    }

    @Test
    void updateGenre_sameName_updatesSuccessfully() {
        Genre existing = genre(1L, "Action");

        given(genreRepository.findById(1L)).willReturn(Optional.of(existing));
        given(genreRepository.save(any(Genre.class))).willReturn(existing);

        Genre result = genreService.updateGenre(1L, genre(null, "Action"));

        assertThat(result.getName()).isEqualTo("Action");
    }

    @Test
    void updateGenre_notFound_throwsGenreNotFoundException() {
        given(genreRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.updateGenre(99L, genre(null, "X")))
                .isInstanceOf(GenreNotFoundException.class);
    }

    @Test
    void updateGenre_duplicateName_throwsIllegalArgumentException() {
        Genre existing = genre(1L, "Action");
        given(genreRepository.findById(1L)).willReturn(Optional.of(existing));
        given(genreRepository.existsByNameIgnoreCase("Drama")).willReturn(true);

        assertThatThrownBy(() -> genreService.updateGenre(1L, genre(null, "Drama")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteGenre ───────────────────────────────────────────────────────────

    @Test
    void deleteGenre_existingId_callsDelete() {
        given(genreRepository.existsById(1L)).willReturn(true);

        genreService.deleteGenre(1L);

        then(genreRepository).should().deleteById(1L);
    }

    @Test
    void deleteGenre_notFound_throwsGenreNotFoundException() {
        given(genreRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> genreService.deleteGenre(99L))
                .isInstanceOf(GenreNotFoundException.class);

        then(genreRepository).should(never()).deleteById(any());
    }

    // ── findGenreByName ───────────────────────────────────────────────────────

    @Test
    void findGenreByName_returnsMatchingPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Genre g = genre(1L, "Action");
        Page<Genre> page = new PageImpl<>(List.of(g));

        given(genreRepository.findByNameContainingIgnoreCase("act", pageable)).willReturn(page);
        given(genreMapper.toResponseDTO(g)).willReturn(dto(1L, "Action"));

        Page<GenreResponseDTO> result = genreService.findGenreByName("act", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findGenreByName_emptyName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> genreService.findGenreByName("", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── findGenreByExactName ──────────────────────────────────────────────────

    @Test
    void findGenreByExactName_existing_returnsDTO() {
        Genre g = genre(1L, "Action");
        given(genreRepository.findByNameIgnoreCase("Action")).willReturn(Optional.of(g));
        given(genreMapper.toResponseDTO(g)).willReturn(dto(1L, "Action"));

        GenreResponseDTO result = genreService.findGenreByExactName("Action");

        assertThat(result.getName()).isEqualTo("Action");
    }

    @Test
    void findGenreByExactName_notFound_throwsGenreNotFoundException() {
        given(genreRepository.findByNameIgnoreCase("Unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.findGenreByExactName("Unknown"))
                .isInstanceOf(GenreNotFoundException.class);
    }
}
