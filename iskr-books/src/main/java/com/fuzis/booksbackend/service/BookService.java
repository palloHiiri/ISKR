package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.Author;
import com.fuzis.booksbackend.entity.Book;
import com.fuzis.booksbackend.entity.Genre;
import com.fuzis.booksbackend.repository.AuthorRepository;
import com.fuzis.booksbackend.repository.BookRepository;
import com.fuzis.booksbackend.repository.GenreRepository;
import com.fuzis.booksbackend.transfer.BookCreateDTO;
import com.fuzis.booksbackend.transfer.BookUpdateDTO;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.state.State;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    public ChangeDTO<Object> createBook(BookCreateDTO dto) {
        try {
            log.info("Creating book with title: {}", dto.getTitle());

            // Validate ISBN uniqueness
            if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {
                if (bookRepository.existsByIsbn(dto.getIsbn())) {
                    log.warn("Book with ISBN {} already exists", dto.getIsbn());
                    return new ChangeDTO<>(State.Fail_Conflict,
                            "Book with this ISBN already exists", null);
                }
            }

            // Fetch authors by IDs
            List<Author> authors = authorRepository.findByAuthorIdIn(
                    dto.getAuthorIds().stream().toList()
            );

            if (authors.size() != dto.getAuthorIds().size()) {
                log.warn("Some authors not found for IDs: {}", dto.getAuthorIds());
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Some authors not found", null);
            }

            // Fetch genres by IDs
            List<Genre> genres = genreRepository.findByGenreIdIn(
                    dto.getGenreIds().stream().toList()
            );

            if (genres.size() != dto.getGenreIds().size()) {
                log.warn("Some genres not found for IDs: {}", dto.getGenreIds());
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Some genres not found", null);
            }

            // Create book entity
            Book book = Book.builder()
                    .title(dto.getTitle())
                    .subtitle(dto.getSubtitle())
                    .isbn(dto.getIsbn())
                    .description(dto.getDescription())
                    .pageCnt(dto.getPageCnt())
                    .photoLink(dto.getPhotoLink())
                    .addedBy(dto.getAddedBy())
                    .authors(new HashSet<>(authors))
                    .genres(new HashSet<>(genres))
                    .build();

            Book savedBook = bookRepository.save(book);
            log.info("Book created successfully with ID: {}", savedBook.getBookId());

            return new ChangeDTO<>(State.OK,
                    "Book created successfully", savedBook);

        } catch (Exception e) {
            log.error("Error creating book: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error creating book: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getBookById(Integer id) {
        try {
            log.debug("Fetching book with ID: {}", id);

            return bookRepository.findByIdWithAuthorsAndGenres(id)
                    .map(book -> {
                        log.debug("Book found with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Book retrieved successfully", (Object)book);
                    })
                    .orElseGet(() -> {
                        log.warn("Book not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Book not found", null);
                    });
        } catch (Exception e) {
            log.error("Error retrieving book with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving book", null);
        }
    }

    public ChangeDTO<Object> updateBook(Integer id, BookUpdateDTO dto) {
        try {
            log.info("Updating book with ID: {}", id);

            return bookRepository.findById(id)
                    .map(book -> {
                        // Update fields if provided
                        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                            book.setTitle(dto.getTitle());
                        }
                        if (dto.getSubtitle() != null) {
                            book.setSubtitle(dto.getSubtitle());
                        }
                        if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {
                            // Check ISBN uniqueness if changed
                            if (!dto.getIsbn().equals(book.getIsbn()) &&
                                    bookRepository.existsByIsbn(dto.getIsbn())) {
                                log.warn("Book with ISBN {} already exists", dto.getIsbn());
                                return new ChangeDTO<>(State.Fail_Conflict,
                                        "Book with this ISBN already exists", null);
                            }
                            book.setIsbn(dto.getIsbn());
                        }
                        if (dto.getDescription() != null) {
                            book.setDescription(dto.getDescription());
                        }
                        if (dto.getPageCnt() != null) {
                            book.setPageCnt(dto.getPageCnt());
                        }
                        if (dto.getPhotoLink() != null) {
                            book.setPhotoLink(dto.getPhotoLink());
                        }

                        // Update authors if provided
                        if (dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty()) {
                            List<Author> authors = authorRepository.findByAuthorIdIn(
                                    dto.getAuthorIds().stream().toList()
                            );
                            if (authors.size() != dto.getAuthorIds().size()) {
                                log.warn("Some authors not found for IDs: {}", dto.getAuthorIds());
                                return new ChangeDTO<>(State.Fail_NotFound,
                                        "Some authors not found", null);
                            }
                            book.setAuthors(new HashSet<>(authors));
                        }

                        // Update genres if provided
                        if (dto.getGenreIds() != null && !dto.getGenreIds().isEmpty()) {
                            List<Genre> genres = genreRepository.findByGenreIdIn(
                                    dto.getGenreIds().stream().toList()
                            );
                            if (genres.size() != dto.getGenreIds().size()) {
                                log.warn("Some genres not found for IDs: {}", dto.getGenreIds());
                                return new ChangeDTO<>(State.Fail_NotFound,
                                        "Some genres not found", null);
                            }
                            book.setGenres(new HashSet<>(genres));
                        }

                        Book updatedBook = bookRepository.save(book);
                        log.info("Book updated successfully with ID: {}", id);
                        return new ChangeDTO<>(State.OK,
                                "Book updated successfully", (Object)updatedBook);
                    })
                    .orElseGet(() -> {
                        log.warn("Book not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Book not found", null);
                    });

        } catch (Exception e) {
            log.error("Error updating book with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error updating book: " + e.getMessage(), null);
        }
    }
    
    public ChangeDTO<Object> deleteBook(Integer id) {
        try {
            log.info("Deleting book with ID: {}", id);

            if (!bookRepository.existsById(id)) {
                log.warn("Book not found with ID: {}", id);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Book not found", null);
            }

            bookRepository.deleteById(id);
            log.info("Book deleted successfully with ID: {}", id);
            return new ChangeDTO<>(State.OK,
                    "Book deleted successfully", null);

        } catch (Exception e) {
            log.error("Error deleting book with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error deleting book: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> getAllBooks(Integer page, Integer batch) {
        try {
            log.debug("Fetching all books, page: {}, batch: {}", page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<Book> booksPage = bookRepository.findAllWithAuthorsAndGenres(pageable);

            // Create simplified response
            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", booksPage.getTotalPages());
            response.put("totalElements", booksPage.getTotalElements());
            response.put("content", booksPage.getContent());

            if (booksPage.isEmpty()) {
                log.debug("No books found");
                return new ChangeDTO<>(State.OK,
                        "No books found", response);
            }

            log.debug("Retrieved {} books", booksPage.getNumberOfElements());
            return new ChangeDTO<>(State.OK,
                    "Books retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving books list: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving books: " + e.getMessage(), null);
        }
    }
}