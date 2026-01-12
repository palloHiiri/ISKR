package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.Genre;
import com.fuzis.booksbackend.repository.GenreRepository;
import com.fuzis.booksbackend.transfer.GenreCreateDTO;
import com.fuzis.booksbackend.transfer.GenreUpdateDTO;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    @Transactional
    public ChangeDTO<Object> createGenre(GenreCreateDTO dto) {
        try {
            log.info("Creating genre with name: {}", dto.getName());

            if (genreRepository.existsByName(dto.getName())) {
                log.warn("Genre with name '{}' already exists", dto.getName());
                return new ChangeDTO<>(State.Fail_Conflict,
                        "Genre with this name already exists", null);
            }

            Genre genre = Genre.builder()
                    .name(dto.getName())
                    .build();

            Genre savedGenre = genreRepository.save(genre);
            log.info("Genre created successfully with ID: {}", savedGenre.getGenreId());

            return new ChangeDTO<>(State.OK,
                    "Genre created successfully", savedGenre);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating genre: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error creating genre: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error creating genre: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getGenreById(Integer id) {
        try {
            log.debug("Fetching genre with ID: {}", id);

            return genreRepository.findById(id)
                    .map(genre -> {
                        log.debug("Genre found with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Genre retrieved successfully", (Object) genre);
                    })
                    .orElseGet(() -> {
                        log.warn("Genre not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Genre not found", null);
                    });
        } catch (Exception e) {
            log.error("Error retrieving genre with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving genre", null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateGenre(Integer id, GenreUpdateDTO dto) {
        try {
            log.info("Updating genre with ID: {}", id);

            return genreRepository.findById(id)
                    .map(genre -> {
                        if (dto.getName() != null && !dto.getName().isBlank()
                                && !dto.getName().equals(genre.getName())) {
                            if (genreRepository.existsByNameAndGenreIdNot(dto.getName(), id)) {
                                log.warn("Genre with name '{}' already exists (excluding current genre)",
                                        dto.getName());
                                return new ChangeDTO<>(State.Fail_Conflict,
                                        "Genre with this name already exists", null);
                            }
                            genre.setName(dto.getName());
                        }

                        Genre updatedGenre = genreRepository.save(genre);
                        log.info("Genre updated successfully with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Genre updated successfully", (Object) updatedGenre);
                    })
                    .orElseGet(() -> {
                        log.warn("Genre not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Genre not found", null);
                    });

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating genre: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating genre with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error updating genre: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> deleteGenre(Integer id) {
        try {
            log.info("Deleting genre with ID: {}", id);

            if (!genreRepository.existsById(id)) {
                log.warn("Genre not found with ID: {}", id);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Genre not found", null);
            }

            genreRepository.deleteById(id);
            log.info("Genre deleted successfully with ID: {}", id);
            return new ChangeDTO<>(State.OK,
                    "Genre deleted successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when deleting genre: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error deleting genre with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error deleting genre: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getAllGenres(Integer page, Integer batch) {
        try {
            log.debug("Fetching all genres, page: {}, batch: {}", page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<Genre> genresPage = genreRepository.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", genresPage.getTotalPages());
            response.put("totalElements", genresPage.getTotalElements());
            response.put("content", genresPage.getContent());

            if (genresPage.isEmpty()) {
                log.debug("No genres found");
                return new ChangeDTO<>(State.OK,
                        "No genres found", response);
            }

            log.debug("Retrieved {} genres", genresPage.getNumberOfElements());
            return new ChangeDTO<>(State.OK,
                    "Genres retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving genres list: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving genres: " + e.getMessage(), null);
        }
    }

    private ChangeDTO<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message.contains("unique constraint") && message.contains("genres_name_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Genre with this name already exists", null);
        } else {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Data integrity violation: " + message, null);
        }
    }
}