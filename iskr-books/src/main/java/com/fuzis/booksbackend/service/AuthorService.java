package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.Author;
import com.fuzis.booksbackend.repository.AuthorRepository;
import com.fuzis.booksbackend.transfer.AuthorCreateDTO;
import com.fuzis.booksbackend.transfer.AuthorUpdateDTO;
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
public class AuthorService {
    private final AuthorRepository authorRepository;

    @Transactional
    public ChangeDTO<Object> createAuthor(AuthorCreateDTO dto) {
        try {
            log.info("Creating author with name: {}", dto.getName());

            Author author = Author.builder()
                    .name(dto.getName())
                    .realName(dto.getRealName())
                    .description(dto.getDescription())
                    .build();

            Author savedAuthor = authorRepository.save(author);
            log.info("Author created successfully with ID: {}", savedAuthor.getAuthorId());

            return new ChangeDTO<>(State.OK,
                    "Author created successfully", savedAuthor);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating author: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error creating author: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error creating author: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getAuthorById(Integer id) {
        try {
            log.debug("Fetching author with ID: {}", id);

            return authorRepository.findById(id)
                    .map(author -> {
                        log.debug("Author found with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Author retrieved successfully", (Object) author);
                    })
                    .orElseGet(() -> {
                        log.warn("Author not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Author not found", null);
                    });
        } catch (Exception e) {
            log.error("Error retrieving author with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving author", null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateAuthor(Integer id, AuthorUpdateDTO dto) {
        try {
            log.info("Updating author with ID: {}", id);

            return authorRepository.findById(id)
                    .map(author -> {
                        if (dto.getName() != null && !dto.getName().isBlank()) {
                            author.setName(dto.getName());
                        }
                        if (dto.getRealName() != null) {
                            author.setRealName(dto.getRealName());
                        }
                        if (dto.getDescription() != null) {
                            author.setDescription(dto.getDescription());
                        }

                        Author updatedAuthor = authorRepository.save(author);
                        log.info("Author updated successfully with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Author updated successfully", (Object) updatedAuthor);
                    })
                    .orElseGet(() -> {
                        log.warn("Author not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Author not found", null);
                    });

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating author: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating author with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error updating author: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> deleteAuthor(Integer id) {
        try {
            log.info("Deleting author with ID: {}", id);

            if (!authorRepository.existsById(id)) {
                log.warn("Author not found with ID: {}", id);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Author not found", null);
            }

            authorRepository.deleteById(id);
            log.info("Author deleted successfully with ID: {}", id);
            return new ChangeDTO<>(State.OK,
                    "Author deleted successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when deleting author: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error deleting author with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error deleting author: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getAllAuthors(Integer page, Integer batch) {
        try {
            log.debug("Fetching all authors, page: {}, batch: {}", page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<Author> authorsPage = authorRepository.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", authorsPage.getTotalPages());
            response.put("totalElements", authorsPage.getTotalElements());
            response.put("content", authorsPage.getContent());

            if (authorsPage.isEmpty()) {
                log.debug("No authors found");
                return new ChangeDTO<>(State.OK,
                        "No authors found", response);
            }

            log.debug("Retrieved {} authors", authorsPage.getNumberOfElements());
            return new ChangeDTO<>(State.OK,
                    "Authors retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving authors list: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving authors: " + e.getMessage(), null);
        }
    }

    private ChangeDTO<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return new ChangeDTO<>(State.Fail_Conflict,
                "Data integrity violation: " + message, null);
    }
}