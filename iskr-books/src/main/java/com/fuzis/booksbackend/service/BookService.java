package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.*;
import com.fuzis.booksbackend.repository.*;
import com.fuzis.booksbackend.transfer.*;
import com.fuzis.booksbackend.transfer.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;
    private final ImageLinkRepository imageLinkRepository;
    private final BooksBookCollectionsRepository booksBookCollectionsRepository;
    private final BookReviewRepository bookReviewRepository;
    private final SubscriberRepository subscriberRepository;

    @Transactional
    public ChangeDTO<Object> createBook(BookCreateDTO dto) {
        try {
            log.info("Creating book with title: {}", dto.getTitle());

            // Validate ISBN uniqueness if provided
            if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {
                if (bookRepository.existsByIsbn(dto.getIsbn())) {
                    log.warn("Book with ISBN {} already exists", dto.getIsbn());
                    return new ChangeDTO<>(State.Fail_Conflict,
                            "Book with this ISBN already exists", null);
                }
            }

            // Validate photoLink uniqueness if provided
            if (dto.getPhotoLink() != null) {
                Optional<ImageLink> photoLinkOpt = imageLinkRepository.findById(dto.getPhotoLink());
                if (photoLinkOpt.isPresent() && bookRepository.existsByPhotoLink_ImglId(photoLinkOpt.get().getImglId())) {
                    log.warn("Book with photoLink {} already exists", dto.getPhotoLink());
                    return new ChangeDTO<>(State.Fail_Conflict,
                            "Book with this photo link already exists", null);
                }
            }

            // Validate unique constraint (title, subtitle)
            if (bookRepository.existsByTitleAndSubtitle(dto.getTitle(), dto.getSubtitle())) {
                log.warn("Book with title '{}' and subtitle '{}' already exists",
                        dto.getTitle(), dto.getSubtitle());
                return new ChangeDTO<>(State.Fail_Conflict,
                        "A book with this title and subtitle combination already exists", null);
            }

            // Fetch addedBy user
            Optional<User> addedByUserOpt = userRepository.findById(dto.getAddedBy());
            if (addedByUserOpt.isEmpty()) {
                log.warn("User not found with ID: {}", dto.getAddedBy());
                return new ChangeDTO<>(State.Fail_NotFound,
                        "User with specified ID does not exist", null);
            }

            // Fetch photoLink if provided
            ImageLink photoLink = null;
            if (dto.getPhotoLink() != null) {
                Optional<ImageLink> photoLinkOpt = imageLinkRepository.findById(dto.getPhotoLink());
                if (photoLinkOpt.isEmpty()) {
                    log.warn("ImageLink not found with ID: {}", dto.getPhotoLink());
                    return new ChangeDTO<>(State.Fail_NotFound,
                            "Image with specified photo link does not exist", null);
                }
                photoLink = photoLinkOpt.get();
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
                    .photoLink(photoLink)
                    .addedBy(addedByUserOpt.get())
                    .authors(new HashSet<>(authors))
                    .genres(new HashSet<>(genres))
                    .build();

            Book savedBook = bookRepository.save(book);
            log.info("Book created successfully with ID: {}", savedBook.getBookId());

            return new ChangeDTO<>(State.OK,
                    "Book created successfully", savedBook);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating book: ", e);
            return handleDataIntegrityViolation(e);
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
                                "Book retrieved successfully", (Object) book);
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

    @Transactional
    public ChangeDTO<Object> updateBook(Integer id, BookUpdateDTO dto) {
        try {
            log.info("Updating book with ID: {}", id);

            return bookRepository.findById(id)
                    .map(book -> {
                        // Check if title or subtitle changed and validate uniqueness
                        boolean titleChanged = dto.getTitle() != null && !dto.getTitle().isBlank()
                                && !dto.getTitle().equals(book.getTitle());
                        boolean subtitleChanged = dto.getSubtitle() != null
                                && (book.getSubtitle() == null || !dto.getSubtitle().equals(book.getSubtitle()));

                        if (titleChanged || subtitleChanged) {
                            String newTitle = titleChanged ? dto.getTitle() : book.getTitle();
                            String newSubtitle = subtitleChanged ? dto.getSubtitle() : book.getSubtitle();

                            if (bookRepository.existsByTitleAndSubtitleAndBookIdNot(newTitle, newSubtitle, id)) {
                                log.warn("Book with title '{}' and subtitle '{}' already exists (excluding current book)",
                                        newTitle, newSubtitle);
                                return new ChangeDTO<>(State.Fail_Conflict,
                                        "A book with this title and subtitle combination already exists", null);
                            }
                        }

                        // Check ISBN uniqueness if changed
                        if (dto.getIsbn() != null && !dto.getIsbn().isBlank()
                                && (book.getIsbn() == null || !dto.getIsbn().equals(book.getIsbn()))) {
                            if (bookRepository.existsByIsbn(dto.getIsbn())) {
                                log.warn("Book with ISBN {} already exists", dto.getIsbn());
                                return new ChangeDTO<>(State.Fail_Conflict,
                                        "Book with this ISBN already exists", null);
                            }
                        }

                        // Check photoLink uniqueness if changed
                        if (dto.getPhotoLink() != null) {
                            Optional<ImageLink> newPhotoLinkOpt = imageLinkRepository.findById(dto.getPhotoLink());
                            if (newPhotoLinkOpt.isEmpty()) {
                                log.warn("ImageLink not found with ID: {}", dto.getPhotoLink());
                                return new ChangeDTO<>(State.Fail_NotFound,
                                        "Image with specified photo link does not exist", null);
                            }

                            ImageLink newPhotoLink = newPhotoLinkOpt.get();
                            boolean photoLinkChanged = book.getPhotoLink() == null
                                    || !book.getPhotoLink().getImglId().equals(newPhotoLink.getImglId());

                            if (photoLinkChanged && bookRepository.existsByPhotoLinkAndBookIdNot(newPhotoLink.getImglId(), id)) {
                                log.warn("Book with photoLink {} already exists", dto.getPhotoLink());
                                return new ChangeDTO<>(State.Fail_Conflict,
                                        "Book with this photo link already exists", null);
                            }
                        }

                        // Update fields if provided
                        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                            book.setTitle(dto.getTitle());
                        }
                        if (dto.getSubtitle() != null) {
                            book.setSubtitle(dto.getSubtitle());
                        }
                        if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {
                            book.setIsbn(dto.getIsbn());
                        }
                        if (dto.getDescription() != null) {
                            book.setDescription(dto.getDescription());
                        }
                        if (dto.getPageCnt() != null) {
                            book.setPageCnt(dto.getPageCnt());
                        }

                        // Update photoLink if provided
                        if (dto.getPhotoLink() != null) {
                            Optional<ImageLink> photoLinkOpt = imageLinkRepository.findById(dto.getPhotoLink());
                            if (photoLinkOpt.isPresent()) {
                                book.setPhotoLink(photoLinkOpt.get());
                            } else {
                                book.setPhotoLink(null);
                            }
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
                                "Book updated successfully", (Object) updatedBook);
                    })
                    .orElseGet(() -> {
                        log.warn("Book not found with ID: {}", id);
                        return new ChangeDTO<>(State.Fail_NotFound,
                                "Book not found", null);
                    });

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating book: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating book with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail,
                    "Error updating book: " + e.getMessage(), null);
        }
    }

    @Transactional
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

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when deleting book: ", e);
            return handleDataIntegrityViolation(e);
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

    private ChangeDTO<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message.contains("added_by")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "User with specified ID does not exist", null);
        } else if (message.contains("photo_link")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "Image with specified photo link does not exist", null);
        } else if (message.contains("unique constraint") && message.contains("books_title_subtitle_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "A book with this title and subtitle combination already exists", null);
        } else if (message.contains("unique constraint") && message.contains("books_isbn_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Book with this ISBN already exists", null);
        } else if (message.contains("unique constraint") && message.contains("books_photo_link_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Book with this photo link already exists", null);
        } else {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Data integrity violation: " + message, null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getBookDetail(Integer id) {
        try {
            log.debug("Fetching detailed book with ID: {}", id);

            Optional<Book> bookOpt = bookRepository.findByIdWithAuthorsAndGenres(id);
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", id);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            Book book = bookOpt.get();

            // Получаем количество добавлений в коллекции
            Long collectionsCount = getCollectionsCountForBook(id);

            // Получаем средний рейтинг (используем исправленный метод)
            Double averageRating = bookReviewRepository.findAverageRatingByBookId(id).orElse(null);

            // Получаем количество отзывов (используем новый метод)
            Long reviewsCount = bookReviewRepository.countByBookId(id);

            // Преобразуем в DTO
            BookDetailDTO bookDetailDTO = convertToBookDetailDTO(book, collectionsCount, averageRating, reviewsCount.intValue());

            log.debug("Book detail retrieved for ID: {}", id);
            return new ChangeDTO<>(State.OK, "Book detail retrieved successfully", bookDetailDTO);

        } catch (Exception e) {
            log.error("Error retrieving book detail with ID {}: ", id, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving book detail: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getBookReviews(Integer bookId, Integer page, Integer batch) {
        try {
            log.debug("Fetching reviews for book ID: {}, page: {}, batch: {}", bookId, page, batch);

            // Проверяем существование книги
            if (!bookRepository.existsById(bookId)) {
                log.warn("Book not found with ID: {}", bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<BookReview> reviewsPage = bookReviewRepository.findByBook_BookId(bookId, pageable);

            // Преобразуем в DTO
            List<BookReviewDTO> reviewDTOs = reviewsPage.getContent().stream()
                    .map(this::convertToBookReviewDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("bookId", bookId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", reviewsPage.getTotalPages());
            response.put("totalElements", reviewsPage.getTotalElements());
            response.put("reviews", reviewDTOs);

            if (reviewDTOs.isEmpty()) {
                log.debug("No reviews found for book {}", bookId);
                return new ChangeDTO<>(State.OK, "No reviews found", response);
            }

            log.debug("Found {} reviews for book {}", reviewDTOs.size(), bookId);
            return new ChangeDTO<>(State.OK, "Reviews retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving reviews for book ID {}: ", bookId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving reviews: " + e.getMessage(), null);
        }
    }

    private BookReviewDTO convertToBookReviewDTO(BookReview review) {
        BookReviewDTO dto = new BookReviewDTO();
        dto.setReviewId(review.getRvwId());
        dto.setScore(review.getScore());
        dto.setReviewText(review.getReviewText());

        // Информация о пользователе
        User user = review.getUser();
        if (user != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(user.getUserId());
            userDTO.setUsername(user.getUsername());
            userDTO.setRegisteredDate(user.getRegisteredDate());

            // Загружаем профиль пользователя
            List<User> usersWithProfiles = userRepository.findByIdsWithProfiles(List.of(user.getUserId()));
            if (!usersWithProfiles.isEmpty() && usersWithProfiles.get(0).getProfile() != null) {
                UserProfile profile = usersWithProfiles.get(0).getProfile();
                userDTO.setNickname(profile.getNickname());

                // Загружаем изображение профиля
                if (profile.getUserImglId() != null) {
                    Integer imageLinkId = profile.getUserImglId().getImglId();
                    List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(List.of(imageLinkId));
                    if (!imageLinks.isEmpty()) {
                        ImageLink imageLink = imageLinks.get(0);
                        if (imageLink.getImageData() != null) {
                            ImageData imageData = imageLink.getImageData();
                            ImageDataDTO imageDataDTO = new ImageDataDTO(
                                    imageData.getImgdId(),
                                    imageData.getUuid(),
                                    imageData.getSize(),
                                    imageData.getMimeType(),
                                    imageData.getExtension()
                            );
                            ImageLinkDTO imageLinkDTO = new ImageLinkDTO(imageLink.getImglId(), imageDataDTO);
                            userDTO.setProfileImage(imageLinkDTO);
                        }
                    }
                }
            }
            dto.setUser(userDTO);
        }

        return dto;
    }

    private Long getCollectionsCountForBook(Integer bookId) {
        try {
            return booksBookCollectionsRepository.countByBookId(bookId);
        } catch (Exception e) {
            log.error("Error getting collections count for book {}: ", bookId, e);
            return 0L;
        }
    }

    private BookDetailDTO convertToBookDetailDTO(Book book, Long collectionsCount, Double averageRating, Integer reviewsCount) {
        BookDetailDTO dto = new BookDetailDTO();
        dto.setBookId(book.getBookId());
        dto.setIsbn(book.getIsbn());
        dto.setTitle(book.getTitle());
        dto.setSubtitle(book.getSubtitle());
        dto.setDescription(book.getDescription());
        dto.setPageCnt(book.getPageCnt());
        dto.setCollectionsCount(collectionsCount);
        dto.setAverageRating(averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : null);
        dto.setReviewsCount(reviewsCount);

        // Загружаем изображение книги
        if (book.getPhotoLink() != null) {
            Integer imageLinkId = book.getPhotoLink().getImglId();
            List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(List.of(imageLinkId));
            if (!imageLinks.isEmpty()) {
                ImageLink imageLink = imageLinks.get(0);
                if (imageLink.getImageData() != null) {
                    ImageData imageData = imageLink.getImageData();
                    ImageDataDTO imageDataDTO = new ImageDataDTO(
                            imageData.getImgdId(),
                            imageData.getUuid(),
                            imageData.getSize(),
                            imageData.getMimeType(),
                            imageData.getExtension()
                    );
                    ImageLinkDTO imageLinkDTO = new ImageLinkDTO(imageLink.getImglId(), imageDataDTO);
                    dto.setPhotoLink(imageLinkDTO);
                }
            }
        }

        // Информация о пользователе, добавившем книгу
        if (book.getAddedBy() != null) {
            List<User> users = userRepository.findByIdsWithProfiles(List.of(book.getAddedBy().getUserId()));
            if (!users.isEmpty()) {
                User user = users.get(0);
                UserDTO userDTO = new UserDTO();
                userDTO.setUserId(user.getUserId());
                userDTO.setUsername(user.getUsername());
                userDTO.setRegisteredDate(user.getRegisteredDate());

                if (user.getProfile() != null) {
                    userDTO.setNickname(user.getProfile().getNickname());

                    // Загружаем изображение профиля
                    if (user.getProfile().getUserImglId() != null) {
                        Integer profileImageLinkId = user.getProfile().getUserImglId().getImglId();
                        List<ImageLink> profileImageLinks = imageLinkRepository.findByIdsWithImageData(List.of(profileImageLinkId));
                        if (!profileImageLinks.isEmpty()) {
                            ImageLink profileImageLink = profileImageLinks.get(0);
                            if (profileImageLink.getImageData() != null) {
                                ImageData imageData = profileImageLink.getImageData();
                                ImageDataDTO imageDataDTO = new ImageDataDTO(
                                        imageData.getImgdId(),
                                        imageData.getUuid(),
                                        imageData.getSize(),
                                        imageData.getMimeType(),
                                        imageData.getExtension()
                                );
                                ImageLinkDTO imageLinkDTO = new ImageLinkDTO(profileImageLink.getImglId(), imageDataDTO);
                                userDTO.setProfileImage(imageLinkDTO);
                            }
                        }
                    }
                }
                dto.setAddedBy(userDTO);
            }
        }

        // Авторы
        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            List<AuthorDetailDTO> authorDTOs = book.getAuthors().stream()
                    .map(author -> new AuthorDetailDTO(
                            author.getAuthorId(),
                            author.getName(),
                            author.getBirthDate(),
                            author.getDescription(),
                            author.getRealName()
                    ))
                    .collect(Collectors.toList());
            dto.setAuthors(authorDTOs);
        }

        // Жанры
        if (book.getGenres() != null && !book.getGenres().isEmpty()) {
            List<GenreDetailDTO> genreDTOs = book.getGenres().stream()
                    .map(genre -> new GenreDetailDTO(
                            genre.getGenreId(),
                            genre.getName()
                    ))
                    .collect(Collectors.toList());
            dto.setGenres(genreDTOs);
        }

        return dto;
    }

    private Map<String, Object> createReviewsResponse(Integer bookId, Integer page, Integer batch,
                                                      List<BookReviewDTO> reviews, int totalReviews) {
        Map<String, Object> response = new HashMap<>();
        response.put("bookId", bookId);
        response.put("page", page);
        response.put("batch", batch);
        response.put("totalPages", (int) Math.ceil((double) totalReviews / batch));
        response.put("totalElements", totalReviews);
        response.put("reviews", reviews);
        return response;
    }
}