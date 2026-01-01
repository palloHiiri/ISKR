package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.*;
import com.fuzis.booksbackend.repository.*;
import com.fuzis.booksbackend.transfer.*;
import com.fuzis.booksbackend.transfer.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class CollectionService {

    private final BookCollectionRepository bookCollectionRepository;
    private final BooksBookCollectionsRepository booksBookCollectionsRepository;
    private final LikedCollectionRepository likedCollectionRepository;
    private final BookRepository bookRepository;
    private final ImageLinkRepository imageLinkRepository;
    private final UserRepository userRepository;
    private final BookReviewRepository bookReviewRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final CollectionAccessRepository collectionAccessRepository;

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getCollectionDetail(Integer collectionId, Integer userId) {
        try {
            log.debug("Getting collection details for ID: {}, userId: {}", collectionId, userId);

            // Проверяем доступ к коллекции
            Boolean canView = checkCollectionAccess(collectionId, userId);
            if (Boolean.FALSE.equals(canView)) {
                log.warn("Access denied to collection {} for user {}", collectionId, userId);
                return new ChangeDTO<>(State.Fail_Forbidden, "Access to collection denied", null);
            }

            // Получаем коллекцию с владельцем и фото
            Optional<BookCollection> collectionOpt = bookCollectionRepository.findByIdWithOwnerAndPhoto(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            // Получаем количество книг в коллекции
            List<Object[]> bookCounts = booksBookCollectionsRepository.findBookCountsByCollectionIds(List.of(collectionId));
            Long booksCount = bookCounts.isEmpty() ? 0L : (Long) bookCounts.get(0)[1];

            // Получаем количество лайков на коллекции
            List<Object[]> likesResults = likedCollectionRepository.findPopularCollections();
            Map<Integer, Long> likesMap = likesResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));
            Long likesCount = likesMap.getOrDefault(collectionId, 0L);

            // Получаем информацию о владельце
            String ownerNickname = null;
            if (collection.getOwner() != null) {
                List<User> owners = userRepository.findByIdsWithProfiles(List.of(collection.getOwner().getUserId()));
                if (!owners.isEmpty() && owners.get(0).getProfile() != null) {
                    ownerNickname = owners.get(0).getProfile().getNickname();
                }
            }

            // Получаем изображение коллекции
            ImageLinkDTO photoLinkDTO = null;
            if (collection.getPhotoLink() != null) {
                Integer imageLinkId = collection.getPhotoLink().getImglId();
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
                        photoLinkDTO = new ImageLinkDTO(imageLink.getImglId(), imageDataDTO);
                    }
                }
            }

            // Создаем DTO
            CollectionDetailDTO dto = new CollectionDetailDTO();
            dto.setCollectionId(collection.getBcolsId());
            dto.setTitle(collection.getTitle());
            dto.setDescription(collection.getDescription());
            dto.setConfidentiality(collection.getConfidentiality().name());
            dto.setCollectionType(collection.getCollectionType().name());
            dto.setPhotoLink(photoLinkDTO);
            dto.setOwnerId(collection.getOwner() != null ? collection.getOwner().getUserId() : null);
            dto.setOwnerNickname(ownerNickname);
            dto.setBooksCount(booksCount);
            dto.setLikesCount(likesCount);
            dto.setCanView(true); // Если дошли сюда, значит доступ есть

            log.debug("Collection details retrieved for ID: {}", collectionId);
            return new ChangeDTO<>(State.OK, "Collection details retrieved successfully", dto);

        } catch (Exception e) {
            log.error("Error retrieving collection details for ID {}: ", collectionId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving collection details: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getCollectionBooks(Integer collectionId, Integer userId, Integer page, Integer batch) {
        try {
            log.debug("Getting books for collection ID: {}, userId: {}, page: {}, batch: {}",
                    collectionId, userId, page, batch);

            // Проверяем доступ к коллекции
            Boolean canView = checkCollectionAccess(collectionId, userId);
            if (Boolean.FALSE.equals(canView)) {
                log.warn("Access denied to collection {} for user {}", collectionId, userId);
                return new ChangeDTO<>(State.Fail_Forbidden, "Access to collection denied", null);
            }

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);

            // Получаем связи книг с коллекцией
            Page<BooksBookCollections> booksInCollectionPage = getBooksInCollectionPage(collectionId, pageable);

            // Извлекаем ID книг
            List<Integer> bookIds = booksInCollectionPage.getContent().stream()
                    .map(bbc -> bbc.getBook().getBookId())
                    .collect(Collectors.toList());

            // Получаем книги с авторами и жанрами
            List<Book> books = bookRepository.findAllById(bookIds);

            // Получаем средние рейтинги
            List<Object[]> averageRatingsResults = bookReviewRepository.findAverageRatingsByBookIds(bookIds);
            Map<Integer, Double> averageRatingsMap = averageRatingsResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Double) row[1]
                    ));

            // Получаем ID изображений
            List<Integer> imageIds = books.stream()
                    .map(Book::getPhotoLink)
                    .filter(Objects::nonNull)
                    .map(ImageLink::getImglId)
                    .collect(Collectors.toList());

            // Получаем изображения с данными
            Map<Integer, ImageLink> imageLinksMap;
            if (!imageIds.isEmpty()) {
                List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(imageIds);
                imageLinksMap = imageLinks.stream()
                        .collect(Collectors.toMap(ImageLink::getImglId, il -> il));
            } else {
                imageLinksMap = new HashMap<>();
            }

            // Преобразуем в DTO
            List<BookInCollectionDTO> bookDTOs = books.stream()
                    .map(book -> {
                        BookInCollectionDTO dto = new BookInCollectionDTO();
                        dto.setBookId(book.getBookId());
                        dto.setTitle(book.getTitle());
                        dto.setSubtitle(book.getSubtitle());
                        dto.setIsbn(book.getIsbn());
                        dto.setPageCnt(book.getPageCnt());
                        dto.setDescription(book.getDescription());

                        // Средний рейтинг
                        Double avgRating = averageRatingsMap.get(book.getBookId());
                        if (avgRating != null) {
                            dto.setAverageRating(Math.round(avgRating * 100.0) / 100.0);
                        }

                        // Изображение книги
                        ImageLink photoLink = book.getPhotoLink();
                        if (photoLink != null) {
                            ImageLink fullImageLink = imageLinksMap.get(photoLink.getImglId());
                            if (fullImageLink != null && fullImageLink.getImageData() != null) {
                                ImageData imageData = fullImageLink.getImageData();
                                ImageDataDTO imageDataDTO = new ImageDataDTO(
                                        imageData.getImgdId(),
                                        imageData.getUuid(),
                                        imageData.getSize(),
                                        imageData.getMimeType(),
                                        imageData.getExtension()
                                );
                                ImageLinkDTO imageLinkDTO = new ImageLinkDTO(
                                        fullImageLink.getImglId(),
                                        imageDataDTO
                                );
                                dto.setPhotoLink(imageLinkDTO);
                            }
                        }

                        // Авторы
                        if (book.getAuthors() != null) {
                            List<AuthorDTO> authorDTOs = book.getAuthors().stream()
                                    .map(author -> new AuthorDTO(
                                            author.getAuthorId(),
                                            author.getName(),
                                            author.getRealName()
                                    ))
                                    .collect(Collectors.toList());
                            dto.setAuthors(authorDTOs);
                        }

                        // Жанры
                        if (book.getGenres() != null) {
                            List<GenreDTO> genreDTOs = book.getGenres().stream()
                                    .map(genre -> new GenreDTO(
                                            genre.getGenreId(),
                                            genre.getName()
                                    ))
                                    .collect(Collectors.toList());
                            dto.setGenres(genreDTOs);
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            // Сортируем по ID книги для сохранения порядка из пагинации
            Map<Integer, BookInCollectionDTO> bookMap = bookDTOs.stream()
                    .collect(Collectors.toMap(BookInCollectionDTO::getBookId, dto -> dto));

            List<BookInCollectionDTO> sortedBookDTOs = bookIds.stream()
                    .map(bookMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("collectionId", collectionId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", booksInCollectionPage.getTotalPages());
            response.put("totalElements", booksInCollectionPage.getTotalElements());
            response.put("books", sortedBookDTOs);
            response.put("canView", true);

            if (sortedBookDTOs.isEmpty()) {
                log.debug("No books found in collection {}", collectionId);
                return new ChangeDTO<>(State.OK, "No books found in collection", response);
            }

            log.debug("Found {} books in collection {}", sortedBookDTOs.size(), collectionId);
            return new ChangeDTO<>(State.OK, "Books retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving books for collection {}: ", collectionId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving books: " + e.getMessage(), null);
        }
    }

    private Boolean checkCollectionAccess(Integer collectionId, Integer userId) {
        try {
            // Если userId не указан, проверяем только публичные коллекции
            if (userId == null) {
                Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
                return collectionOpt.isPresent() &&
                        collectionOpt.get().getConfidentiality().name().equals("Public");
            }

            // Если userId указан, используем функцию CAN_VIEW_COLLECTION
            return collectionAccessRepository.canViewCollection(userId, collectionId);
        } catch (Exception e) {
            log.error("Error checking collection access: ", e);
            return false;
        }
    }

    private Page<BooksBookCollections> getBooksInCollectionPage(Integer collectionId, Pageable pageable) {
        return booksBookCollectionsRepository.findByBookCollection_BcolsId(collectionId, pageable);
    }
}