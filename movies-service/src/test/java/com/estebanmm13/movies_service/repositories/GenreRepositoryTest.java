package com.estebanmm13.movies_service.repositories;

import com.estebanmm13.movies_service.models.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GenreRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private GenreRepository genreRepository;

    private Genre action;
    private Genre drama;
    private Genre scienceFiction;

    @BeforeEach
    void setUp() {
        action = em.persistFlushFind(Genre.builder().name("Action").build());
        drama = em.persistFlushFind(Genre.builder().name("Drama").build());
        scienceFiction = em.persistFlushFind(Genre.builder().name("Science Fiction").build());
    }

    // ── findByNameIgnoreCase ──────────────────────────────────────────────────

    @Test
    void findByNameIgnoreCase_exactMatch_returnsGenre() {
        Optional<Genre> result = genreRepository.findByNameIgnoreCase("Action");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(action.getId());
    }

    @Test
    void findByNameIgnoreCase_uppercaseInput_returnsGenre() {
        Optional<Genre> result = genreRepository.findByNameIgnoreCase("ACTION");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Action");
    }

    @Test
    void findByNameIgnoreCase_mixedCase_returnsGenre() {
        Optional<Genre> result = genreRepository.findByNameIgnoreCase("aCtIoN");

        assertThat(result).isPresent();
    }

    @Test
    void findByNameIgnoreCase_nonExisting_returnsEmpty() {
        Optional<Genre> result = genreRepository.findByNameIgnoreCase("Horror");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNameIgnoreCase_partialName_returnsEmpty() {
        Optional<Genre> result = genreRepository.findByNameIgnoreCase("Act");

        assertThat(result).isEmpty();
    }

    // ── findByNameContainingIgnoreCase ────────────────────────────────────────

    @Test
    void findByNameContainingIgnoreCase_partialMatch_returnsMatches() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "action", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Action");
    }

    @Test
    void findByNameContainingIgnoreCase_caseInsensitivePartial_returnsMatches() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "DRAMA", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Drama");
    }

    @Test
    void findByNameContainingIgnoreCase_multiWordSearch_returnsMatches() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "science", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Science Fiction");
    }

    @Test
    void findByNameContainingIgnoreCase_noMatch_returnsEmptyPage() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "horror", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_emptyString_returnsAll() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
    }

    // ── existsByNameIgnoreCase ────────────────────────────────────────────────

    @Test
    void existsByNameIgnoreCase_existingName_returnsTrue() {
        assertThat(genreRepository.existsByNameIgnoreCase("Action")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_existingNameUppercase_returnsTrue() {
        assertThat(genreRepository.existsByNameIgnoreCase("ACTION")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_nonExisting_returnsFalse() {
        assertThat(genreRepository.existsByNameIgnoreCase("Horror")).isFalse();
    }

    // ── CRUD básico ───────────────────────────────────────────────────────────

    @Test
    void save_newGenre_persistsSuccessfully() {
        Genre horror = Genre.builder().name("Horror").build();
        Genre saved = genreRepository.save(horror);

        assertThat(saved.getId()).isNotNull();
        assertThat(genreRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_existingGenre_removesFromDatabase() {
        genreRepository.delete(drama);
        em.flush();

        assertThat(genreRepository.findById(drama.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllGenres() {
        assertThat(genreRepository.findAll()).hasSize(3);
    }

    @Test
    void pagination_limitsResults() {
        Page<Genre> result = genreRepository.findByNameContainingIgnoreCase(
                "", PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}
