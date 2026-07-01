/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.estebanmm13.movies_service.repositories;


import com.estebanmm13.movies_service.models.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findMovieByTitleContaining(String title, Pageable pageable);

    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE LOWER(g.name) = LOWER(:name)")
    Page<Movie> findAllByGenreName(@Param("name") String name, Pageable pageable);
}
