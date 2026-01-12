package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.*;
import com.fuzis.booksbackend.entity.enumerate.CollectionType;
import com.fuzis.booksbackend.entity.enumerate.Confidentiality;
import com.fuzis.booksbackend.entity.enumerate.CvpStatus;
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
public class CollectionService {

    private final BookCollectionRepository bookCollectionRepository;
    private final BooksBookCollectionsRepository booksBookCollectionsRepository;
    private final LikedCollectionRepository likedCollectionRepository;
    private final CollectionViewPrivilegeRepository collectionViewPrivilegeRepository;
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

            Boolean canView = checkCollectionAccess(collectionId, userId);
            if (Boolean.FALSE.equals(canView)) {
                log.warn("Access denied to collection {} for user {}", collectionId, userId);
                return new ChangeDTO<>(State.Fail_Forbidden, "Access to collection denied", null);
            }

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findByIdWithOwnerAndPhoto(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            List<Object[]> bookCounts = booksBookCollectionsRepository.findBookCountsByCollectionIds(List.of(collectionId));
            Long booksCount = bookCounts.isEmpty() ? 0L : (Long) bookCounts.get(0)[1];

            List<Object[]> likesResults = likedCollectionRepository.findPopularCollections();
            Map<Integer, Long> likesMap = likesResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));
            Long likesCount = likesMap.getOrDefault(collectionId, 0L);

            String ownerNickname = null;
            if (collection.getOwner() != null) {
                List<User> owners = userRepository.findByIdsWithProfiles(List.of(collection.getOwner().getUserId()));
                if (!owners.isEmpty() && owners.get(0).getProfile() != null) {
                    ownerNickname = owners.get(0).getProfile().getNickname();
                }
            }

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

            CollectionDetailDTO dto = new CollectionDetailDTO();
            dto.setCollectionId(collection.getBcolsId());
            dto.setTitle(collection.getTitle());
            dto.setDescription(collection.getDescription());
            dto.setConfidentiality(String.valueOf(collection.getConfidentiality()));
            dto.setCollectionType(String.valueOf(collection.getCollectionType()));
            dto.setPhotoLink(photoLinkDTO);
            dto.setOwnerId(collection.getOwner() != null ? collection.getOwner().getUserId() : null);
            dto.setOwnerNickname(ownerNickname);
            dto.setBooksCount(booksCount);
            dto.setLikesCount(likesCount);

            dto.setCanView(true);

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

            Page<BooksBookCollections> booksInCollectionPage = getBooksInCollectionPage(collectionId, pageable);

            List<Integer> bookIds = booksInCollectionPage.getContent().stream()
                    .map(bbc -> bbc.getBook().getBookId())
                    .collect(Collectors.toList());

            List<Book> books = bookRepository.findAllById(bookIds);

            List<Object[]> averageRatingsResults = bookReviewRepository.findAverageRatingsByBookIds(bookIds);
            Map<Integer, Double> averageRatingsMap = averageRatingsResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Double) row[1]
                    ));

            List<Integer> imageIds = books.stream()
                    .map(Book::getPhotoLink)
                    .filter(Objects::nonNull)
                    .map(ImageLink::getImglId)
                    .collect(Collectors.toList());

            Map<Integer, ImageLink> imageLinksMap;
            if (!imageIds.isEmpty()) {
                List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(imageIds);
                imageLinksMap = imageLinks.stream()
                        .collect(Collectors.toMap(ImageLink::getImglId, il -> il));
            } else {
                imageLinksMap = new HashMap<>();
            }

            List<BookInCollectionDTO> bookDTOs = books.stream()
                    .map(book -> {
                        BookInCollectionDTO dto = new BookInCollectionDTO();
                        dto.setBookId(book.getBookId());
                        dto.setTitle(book.getTitle());
                        dto.setSubtitle(book.getSubtitle());
                        dto.setIsbn(book.getIsbn());
                        dto.setPageCnt(book.getPageCnt());
                        dto.setDescription(book.getDescription());

                        Double avgRating = averageRatingsMap.get(book.getBookId());
                        if (avgRating != null) {
                            dto.setAverageRating(Math.round(avgRating * 100.0) / 100.0);
                        }

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

    @Transactional
    public ChangeDTO<Object> createCollection(Integer userId, CollectionRequestDTO dto) {
        try {
            log.info("Creating collection by user ID: {}", userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for collection creation");
                return new ChangeDTO<>(State.Fail_Forbidden, "Only registered users can create collections", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            String confidentiality = dto.getConfidentiality();
            if (confidentiality == null || confidentiality.isBlank()) {
                log.warn("Confidentiality is required");
                return new ChangeDTO<>(State.Fail_BadData, "Confidentiality is required", null);
            }

            if (!"Public".equalsIgnoreCase(confidentiality) && !"Private".equalsIgnoreCase(confidentiality)) {
                log.warn("Invalid confidentiality value: {}", confidentiality);
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid confidentiality value. Must be: Public or Private", null);
            }

            String collectionType = dto.getCollectionType();
            if (collectionType == null || collectionType.isBlank()) {
                log.warn("Collection type is required");
                return new ChangeDTO<>(State.Fail_BadData, "Collection type is required", null);
            }

            if (!"Standard".equalsIgnoreCase(collectionType) &&
                    !"Liked".equalsIgnoreCase(collectionType) &&
                    !"Wishlist".equalsIgnoreCase(collectionType)) {
                log.warn("Invalid collection type value: {}", collectionType);
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid collection type value. Must be: Standard, Liked or Wishlist", null);
            }

            if ("Wishlist".equalsIgnoreCase(collectionType)) {
                boolean hasWishlist = bookCollectionRepository.existsWishlistByUserId(userId);
                if (hasWishlist) {
                    log.warn("User {} already has a wishlist", userId);
                    return new ChangeDTO<>(State.Fail_Conflict,
                            "User can only have one wishlist", null);
                }
            }

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

            BookCollection collection = BookCollection.builder()
                    .owner(userOpt.get())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .confidentiality(Confidentiality.valueOf(confidentiality))
                    .collectionType(CollectionType.valueOf(collectionType))
                    .photoLink(photoLink)
                    .build();

            BookCollection savedCollection = bookCollectionRepository.save(collection);
            log.info("Collection created with ID: {}", savedCollection.getBcolsId());

            return getCollectionDetail(savedCollection.getBcolsId(), userId);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error creating collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error creating collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateCollection(Integer userId, Integer collectionId, CollectionRequestDTO dto) {
        try {
            log.info("Updating collection ID: {} by user ID: {}", collectionId, userId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to update collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot update collection");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                collection.setTitle(dto.getTitle());
            }

            if (dto.getDescription() != null) {
                collection.setDescription(dto.getDescription());
            }

            if (dto.getConfidentiality() != null && !dto.getConfidentiality().isBlank()) {
                if (!"Public".equalsIgnoreCase(dto.getConfidentiality()) &&
                        !"Private".equalsIgnoreCase(dto.getConfidentiality())) {
                    log.warn("Invalid confidentiality value: {}", dto.getConfidentiality());
                    return new ChangeDTO<>(State.Fail_BadData,
                            "Invalid confidentiality value. Must be: Public or Private", null);
                }
                collection.setConfidentiality(Confidentiality.valueOf(dto.getConfidentiality()));
            }

            if (dto.getCollectionType() != null && !dto.getCollectionType().isBlank()) {
                if (!"Standard".equalsIgnoreCase(dto.getCollectionType()) &&
                        !"Liked".equalsIgnoreCase(dto.getCollectionType()) &&
                        !"Wishlist".equalsIgnoreCase(dto.getCollectionType())) {
                    log.warn("Invalid collection type value: {}", dto.getCollectionType());
                    return new ChangeDTO<>(State.Fail_BadData,
                            "Invalid collection type value. Must be: Standard, Liked or Wishlist", null);
                }

                if ("Wishlist".equalsIgnoreCase(dto.getCollectionType()) &&
                        !"Wishlist".equalsIgnoreCase(String.valueOf(collection.getCollectionType()))) {

                    Integer ownerId = collection.getOwner() != null ? collection.getOwner().getUserId() : null;
                    if (ownerId != null) {
                        boolean hasWishlist = bookCollectionRepository.existsWishlistByUserId(ownerId);
                        if (hasWishlist) {
                            log.warn("User {} already has a wishlist, cannot convert collection to wishlist", ownerId);
                            return new ChangeDTO<>(State.Fail_Conflict,
                                    "User can only have one wishlist", null);
                        }
                    }
                }

                collection.setCollectionType(CollectionType.valueOf(dto.getCollectionType()));
            }

            if (dto.getPhotoLink() != null) {
                Optional<ImageLink> photoLinkOpt = imageLinkRepository.findById(dto.getPhotoLink());
                if (photoLinkOpt.isPresent()) {
                    collection.setPhotoLink(photoLinkOpt.get());
                } else {
                    log.warn("ImageLink not found with ID: {}", dto.getPhotoLink());
                    return new ChangeDTO<>(State.Fail_NotFound,
                            "Image with specified photo link does not exist", null);
                }
            }

            BookCollection updatedCollection = bookCollectionRepository.save(collection);
            log.info("Collection updated with ID: {}", collectionId);

            return getCollectionDetail(collectionId, userId);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error updating collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> deleteCollection(Integer userId, Integer collectionId) {
        try {
            log.info("Deleting collection ID: {} by user ID: {}", collectionId, userId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to delete collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot delete collection");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            bookCollectionRepository.delete(collection);
            log.info("Collection deleted with ID: {}", collectionId);

            return new ChangeDTO<>(State.OK, "Collection deleted successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when deleting collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error deleting collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error deleting collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> addBookToCollection(Integer userId, Integer collectionId, Integer bookId) {
        try {
            log.info("Adding book {} to collection {} by user {}", bookId, collectionId, userId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to modify collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot add books to collection");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            boolean alreadyExists = booksBookCollectionsRepository.findByBook_BookIdIn(List.of(bookId))
                    .stream()
                    .anyMatch(bbc -> bbc.getBookCollection().getBcolsId().equals(collectionId));

            if (alreadyExists) {
                log.warn("Book {} already exists in collection {}", bookId, collectionId);
                return new ChangeDTO<>(State.Fail_Conflict, "Book already exists in collection", null);
            }

            BooksBookCollections booksBookCollections = BooksBookCollections.builder()
                    .book(bookOpt.get())
                    .bookCollection(collection)
                    .build();

            booksBookCollectionsRepository.save(booksBookCollections);
            log.info("Book {} added to collection {}", bookId, collectionId);

            return new ChangeDTO<>(State.OK, "Book added to collection successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when adding book to collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error adding book to collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error adding book to collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> removeBookFromCollection(Integer userId, Integer collectionId, Integer bookId) {
        try {
            log.info("Removing book {} from collection {} by user {}", bookId, collectionId, userId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to modify collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot remove books from collection");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            List<BooksBookCollections> booksInCollection = booksBookCollectionsRepository.findByBook_BookIdIn(List.of(bookId));
            Optional<BooksBookCollections> bbcOpt = booksInCollection.stream()
                    .filter(bbc -> bbc.getBookCollection().getBcolsId().equals(collectionId))
                    .findFirst();

            if (bbcOpt.isEmpty()) {
                log.warn("Book {} not found in collection {}", bookId, collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found in collection", null);
            }

            booksBookCollectionsRepository.delete(bbcOpt.get());
            log.info("Book {} removed from collection {}", bookId, collectionId);

            return new ChangeDTO<>(State.OK, "Book removed from collection successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when removing book from collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error removing book from collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error removing book from collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> addCollectionPrivilege(Integer userId, Integer collectionId,
                                                    CollectionPrivilegeRequestDTO dto) {
        try {
            log.info("Adding privilege to collection {} by user {} for user {}",
                    collectionId, userId, dto.getUserId());

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to modify privileges for collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot manage collection privileges");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            Optional<User> targetUserOpt = userRepository.findById(dto.getUserId());
            if (targetUserOpt.isEmpty()) {
                log.warn("Target user not found with ID: {}", dto.getUserId());
                return new ChangeDTO<>(State.Fail_NotFound, "Target user not found", null);
            }

            if (collection.getOwner() != null && collection.getOwner().getUserId().equals(dto.getUserId())) {
                log.warn("Cannot set privilege for collection owner");
                return new ChangeDTO<>(State.Fail_Conflict, "Cannot set privilege for collection owner", null);
            }

            String status = dto.getCvpStatus();
            if (!"Allowed".equalsIgnoreCase(status) &&
                    !"Disallowed".equalsIgnoreCase(status)) {
                log.warn("Invalid CVP status value: {}", dto.getCvpStatus());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid privilege status value. Must be: Allowed, Disallowed", null);
            }

            Optional<CollectionViewPrivilege> existingPrivilegeOpt =
                    collectionViewPrivilegeRepository.findByCollectionIdAndUserId(collectionId, dto.getUserId());

            if (existingPrivilegeOpt.isPresent()) {
                CollectionViewPrivilege existingPrivilege = existingPrivilegeOpt.get();
                existingPrivilege.setStatus(CvpStatus.valueOf(status));
                collectionViewPrivilegeRepository.save(existingPrivilege);
                log.info("Updated existing privilege for user {} on collection {}", dto.getUserId(), collectionId);
            } else {
                CollectionViewPrivilege privilege = CollectionViewPrivilege.builder()
                        .bcolsId(collectionId)
                        .userId(dto.getUserId())
                        .status(CvpStatus.valueOf(status))
                        .collection(collection)
                        .build();
                collectionViewPrivilegeRepository.save(privilege);
                log.info("Created new privilege for user {} on collection {}", dto.getUserId(), collectionId);
            }

            return new ChangeDTO<>(State.OK, "Privilege added successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when adding collection privilege: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error adding collection privilege: ", e);
            return new ChangeDTO<>(State.Fail, "Error adding collection privilege: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> removeCollectionPrivilege(Integer userId, Integer collectionId, Integer privilegeUserId) {
        try {
            log.info("Removing privilege from collection {} by user {} for user {}",
                    collectionId, userId, privilegeUserId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to modify privileges for collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot manage collection privileges");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            Optional<CollectionViewPrivilege> privilegeOpt =
                    collectionViewPrivilegeRepository.findByCollectionIdAndUserId(collectionId, privilegeUserId);

            if (privilegeOpt.isEmpty()) {
                log.warn("Privilege not found for user {} on collection {}", privilegeUserId, collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Privilege not found", null);
            }

            collectionViewPrivilegeRepository.delete(privilegeOpt.get());
            log.info("Privilege removed for user {} from collection {}", privilegeUserId, collectionId);

            return new ChangeDTO<>(State.OK, "Privilege removed successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when removing collection privilege: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error removing collection privilege: ", e);
            return new ChangeDTO<>(State.Fail, "Error removing collection privilege: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> likeCollection(Integer userId, Integer collectionId) {
        try {
            log.info("User {} liking collection {}", userId, collectionId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for like operation");
                return new ChangeDTO<>(State.Fail_Forbidden, "Only registered users can like collections", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            Boolean canView = checkCollectionAccess(collectionId, userId);
            if (Boolean.FALSE.equals(canView)) {
                log.warn("User {} cannot view collection {}, cannot like", userId, collectionId);
                return new ChangeDTO<>(State.Fail_Forbidden, "Cannot like a collection you cannot view", null);
            }

            boolean alreadyLiked = likedCollectionRepository.existsByUserIdAndCollectionId(userId, collectionId);
            if (alreadyLiked) {
                log.warn("User {} already liked collection {}", userId, collectionId);
                return new ChangeDTO<>(State.Fail_Conflict, "Collection already liked", null);
            }

            LikedCollection likedCollection = LikedCollection.builder()
                    .user(userOpt.get())
                    .bcols(collectionOpt.get())
                    .build();

            likedCollectionRepository.save(likedCollection);
            log.info("User {} liked collection {}", userId, collectionId);

            return new ChangeDTO<>(State.OK, "Collection liked successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when liking collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error liking collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error liking collection: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> unlikeCollection(Integer userId, Integer collectionId) {
        try {
            log.info("User {} unliking collection {}", userId, collectionId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for unlike operation");
                return new ChangeDTO<>(State.Fail_Forbidden, "Only registered users can unlike collections", null);
            }

            Optional<LikedCollection> likedCollectionOpt =
                    likedCollectionRepository.findByUserIdAndCollectionId(userId, collectionId);

            if (likedCollectionOpt.isEmpty()) {
                log.warn("Like not found for user {} on collection {}", userId, collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Like not found", null);
            }

            likedCollectionRepository.delete(likedCollectionOpt.get());
            log.info("User {} unliked collection {}", userId, collectionId);

            return new ChangeDTO<>(State.OK, "Collection unliked successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when unliking collection: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error unliking collection: ", e);
            return new ChangeDTO<>(State.Fail, "Error unliking collection: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> checkIfLikedCollection(Integer userId, Integer collectionId) {
        try {
            log.debug("Checking if user {} liked collection {}", userId, collectionId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for checking like status");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            boolean isLiked = likedCollectionRepository.existsByUserIdAndCollectionId(userId, collectionId);

            Map<String, Object> response = new HashMap<>();
            response.put("collectionId", collectionId);
            response.put("userId", userId);
            response.put("isLiked", isLiked);

            log.debug("Like check result for user {} on collection {}: {}", userId, collectionId, isLiked);
            return new ChangeDTO<>(State.OK, "Like status retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error checking like status: ", e);
            return new ChangeDTO<>(State.Fail, "Error checking like status: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getCollectionPrivileges(Integer userId, Integer collectionId) {
        try {
            log.debug("Getting privileges for collection ID: {} by user ID: {}", collectionId, userId);

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            BookCollection collection = collectionOpt.get();

            if (userId != null && userId != -1) {
                if (collection.getOwner() == null || !collection.getOwner().getUserId().equals(userId)) {
                    log.warn("User {} has no access to view privileges for collection {}", userId, collectionId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Invalid user", null);
                }
            }
            if (userId != null && userId == -1) {
                log.warn("Unauthorized user cannot view collection privileges");
                return new ChangeDTO<>(State.Fail_Forbidden, "Unauthorized access", null);
            }

            List<CollectionViewPrivilege> privileges = collectionViewPrivilegeRepository
                    .findByBcolsId(collectionId);

            List<CollectionPrivilegeDTO> privilegeDTOs = privileges.stream()
                    .map(this::convertToCollectionPrivilegeDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("collectionId", collectionId);
            response.put("totalPrivileges", privilegeDTOs.size());
            response.put("privileges", privilegeDTOs);

            log.debug("Retrieved {} privileges for collection {}", privilegeDTOs.size(), collectionId);
            return new ChangeDTO<>(State.OK, "Privileges retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving collection privileges: ", e);
            return new ChangeDTO<>(State.Fail, "Error retrieving privileges: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getMyCollections(Integer userId, Integer page, Integer batch) {
        try {
            log.debug("Getting collections for user ID: {}, page: {}, batch: {}", userId, page, batch);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for getting collections");
                return new ChangeDTO<>(State.Fail_Forbidden,
                        "Only registered users can view their collections", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);

            Page<BookCollection> collectionsPage = bookCollectionRepository.findByOwner_UserId(userId, pageable);

            if (collectionsPage.isEmpty()) {
                log.debug("No collections found for user {}", userId);

                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("page", page);
                emptyResponse.put("batch", batch);
                emptyResponse.put("totalPages", collectionsPage.getTotalPages());
                emptyResponse.put("totalElements", collectionsPage.getTotalElements());
                emptyResponse.put("collections", new ArrayList<>());

                return new ChangeDTO<>(State.OK, "No collections found", emptyResponse);
            }

            List<BookCollection> collections = collectionsPage.getContent();
            List<Integer> collectionIds = collections.stream()
                    .map(BookCollection::getBcolsId)
                    .collect(Collectors.toList());

            List<Object[]> bookCountsResults = booksBookCollectionsRepository
                    .findBookCountsByCollectionIds(collectionIds);
            Map<Integer, Long> booksCountMap = bookCountsResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<Object[]> likesResults = likedCollectionRepository.findPopularCollections();
            Map<Integer, Long> likesCountMap = likesResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<Integer> photoLinkIds = collections.stream()
                    .map(BookCollection::getPhotoLink)
                    .filter(Objects::nonNull)
                    .map(ImageLink::getImglId)
                    .collect(Collectors.toList());

            Map<Integer, ImageLink> imageLinksMap;
            if (!photoLinkIds.isEmpty()) {
                List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(photoLinkIds);
                imageLinksMap = imageLinks.stream()
                        .collect(Collectors.toMap(ImageLink::getImglId, il -> il));
            } else {
                imageLinksMap = new HashMap<>();
            }

            List<CollectionSimpleDTO> collectionDTOs = collections.stream()
                    .map(collection -> {
                        CollectionSimpleDTO dto = new CollectionSimpleDTO();
                        dto.setCollectionId(collection.getBcolsId());
                        dto.setTitle(collection.getTitle());
                        dto.setDescription(collection.getDescription());
                        dto.setConfidentiality(String.valueOf(collection.getConfidentiality()));
                        dto.setCollectionType(String.valueOf(collection.getCollectionType()));

                        Long booksCount = booksCountMap.getOrDefault(collection.getBcolsId(), 0L);
                        dto.setBooksCount(booksCount);

                        Long likesCount = likesCountMap.getOrDefault(collection.getBcolsId(), 0L);
                        dto.setLikesCount(likesCount);

                        ImageLink photoLink = collection.getPhotoLink();
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

                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<Integer, CollectionSimpleDTO> collectionMap = collectionDTOs.stream()
                    .collect(Collectors.toMap(CollectionSimpleDTO::getCollectionId, dto -> dto));

            List<CollectionSimpleDTO> sortedCollectionDTOs = collectionIds.stream()
                    .map(collectionMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", collectionsPage.getTotalPages());
            response.put("totalElements", collectionsPage.getTotalElements());
            response.put("collections", sortedCollectionDTOs);

            log.debug("Retrieved {} collections for user {}", sortedCollectionDTOs.size(), userId);
            return new ChangeDTO<>(State.OK, "Collections retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving collections for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving collections: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> checkBookInCollection(Integer userId, Integer collectionId, Integer bookId) {
        try {
            log.debug("Checking if book {} exists in collection {} for user {}", bookId, collectionId, userId);

            if (userId != null && userId != -1) {
                Boolean canView = checkCollectionAccess(collectionId, userId);
                if (Boolean.FALSE.equals(canView)) {
                    log.warn("Access denied to collection {} for user {}", collectionId, userId);
                    return new ChangeDTO<>(State.Fail_Forbidden, "Access to collection denied", null);
                }
            }

            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                log.warn("Collection not found with ID: {}", collectionId);
                return new ChangeDTO<>(State.Fail_NotFound, "Collection not found", null);
            }

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            List<BooksBookCollections> booksInCollection = booksBookCollectionsRepository
                    .findByBook_BookIdIn(List.of(bookId));

            boolean exists = booksInCollection.stream()
                    .anyMatch(bbc -> bbc.getBookCollection().getBcolsId().equals(collectionId));

            Map<String, Object> response = new HashMap<>();
            response.put("collectionId", collectionId);
            response.put("bookId", bookId);
            response.put("exists", exists);

            if (exists) {
                Optional<BooksBookCollections> bbcOpt = booksInCollection.stream()
                        .filter(bbc -> bbc.getBookCollection().getBcolsId().equals(collectionId))
                        .findFirst();

                if (bbcOpt.isPresent()) {
                    BooksBookCollections bbc = bbcOpt.get();
                    response.put("bookCollectionId", bbc.getCBookBcolId());
                }

                Book book = bookOpt.get();
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("title", book.getTitle());
                bookInfo.put("subtitle", book.getSubtitle());
                bookInfo.put("isbn", book.getIsbn());
                response.put("bookInfo", bookInfo);

                BookCollection collection = collectionOpt.get();
                Map<String, Object> collectionInfo = new HashMap<>();
                collectionInfo.put("title", collection.getTitle());
                collectionInfo.put("confidentiality", collection.getConfidentiality());
                collectionInfo.put("collectionType", collection.getCollectionType());
                response.put("collectionInfo", collectionInfo);
            }

            log.debug("Book {} exists in collection {}: {}", bookId, collectionId, exists);
            return new ChangeDTO<>(State.OK,
                    exists ? "Book exists in collection" : "Book does not exist in collection",
                    response);

        } catch (Exception e) {
            log.error("Error checking if book exists in collection: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error checking if book exists in collection: " + e.getMessage(), null);
        }
    }

    private Optional<BookCollection> getWishlistByUserId(Integer userId) {
        return bookCollectionRepository.findWishlistByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> checkUserWishlist(Integer userId) {
        try {
            log.debug("Checking wishlist for user ID: {}", userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for wishlist check");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<BookCollection> wishlistOpt = getWishlistByUserId(userId);
            boolean hasWishlist = wishlistOpt.isPresent();

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("hasWishlist", hasWishlist);

            if (hasWishlist) {
                BookCollection wishlist = wishlistOpt.get();
                response.put("wishlistId", wishlist.getBcolsId());
                response.put("wishlistTitle", wishlist.getTitle());
                response.put("confidentiality", wishlist.getConfidentiality());

                Long booksCount = booksBookCollectionsRepository.countByBookCollection_BcolsId(wishlist.getBcolsId());
                response.put("booksCount", booksCount);

                List<Object[]> likesResults = likedCollectionRepository.findPopularCollections();
                Long likesCount = likesResults.stream()
                        .filter(row -> wishlist.getBcolsId().equals((Integer) row[0]))
                        .map(row -> (Long) row[1])
                        .findFirst()
                        .orElse(0L);
                response.put("likesCount", likesCount);
            }

            log.debug("Wishlist check for user {}: {}", userId, hasWishlist);
            return new ChangeDTO<>(State.OK,
                    hasWishlist ? "User has a wishlist" : "User does not have a wishlist",
                    response);

        } catch (Exception e) {
            log.error("Error checking wishlist for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail, "Error checking wishlist: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> addBookToWishlist(Integer userId, Integer bookId) {
        try {
            log.info("Adding book {} to wishlist for user {}", bookId, userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for adding to wishlist");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<BookCollection> wishlistOpt = getWishlistByUserId(userId);
            if (wishlistOpt.isEmpty()) {
                log.warn("User {} does not have a wishlist", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User does not have a wishlist", null);
            }

            BookCollection wishlist = wishlistOpt.get();

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            boolean alreadyExists = booksBookCollectionsRepository.findByBook_BookIdIn(List.of(bookId))
                    .stream()
                    .anyMatch(bbc -> bbc.getBookCollection().getBcolsId().equals(wishlist.getBcolsId()));

            if (alreadyExists) {
                log.warn("Book {} already exists in wishlist for user {}", bookId, userId);
                return new ChangeDTO<>(State.Fail_Conflict, "Book already exists in wishlist", null);
            }

            BooksBookCollections booksBookCollections = BooksBookCollections.builder()
                    .book(bookOpt.get())
                    .bookCollection(wishlist)
                    .build();

            booksBookCollectionsRepository.save(booksBookCollections);
            log.info("Book {} added to wishlist for user {}", bookId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("wishlistId", wishlist.getBcolsId());
            response.put("bookId", bookId);
            response.put("added", true);

            return new ChangeDTO<>(State.OK, "Book added to wishlist successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when adding book to wishlist: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error adding book to wishlist: ", e);
            return new ChangeDTO<>(State.Fail, "Error adding book to wishlist: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> removeBookFromWishlist(Integer userId, Integer bookId) {
        try {
            log.info("Removing book {} from wishlist for user {}", bookId, userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for removing from wishlist");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<BookCollection> wishlistOpt = getWishlistByUserId(userId);
            if (wishlistOpt.isEmpty()) {
                log.warn("User {} does not have a wishlist", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User does not have a wishlist", null);
            }

            BookCollection wishlist = wishlistOpt.get();

            List<BooksBookCollections> booksInWishlist = booksBookCollectionsRepository
                    .findByBook_BookIdIn(List.of(bookId));

            Optional<BooksBookCollections> bbcOpt = booksInWishlist.stream()
                    .filter(bbc -> bbc.getBookCollection().getBcolsId().equals(wishlist.getBcolsId()))
                    .findFirst();

            if (bbcOpt.isEmpty()) {
                log.warn("Book {} not found in wishlist for user {}", bookId, userId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found in wishlist", null);
            }

            booksBookCollectionsRepository.delete(bbcOpt.get());
            log.info("Book {} removed from wishlist for user {}", bookId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("wishlistId", wishlist.getBcolsId());
            response.put("bookId", bookId);
            response.put("removed", true);

            return new ChangeDTO<>(State.OK, "Book removed from wishlist successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when removing book from wishlist: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error removing book from wishlist: ", e);
            return new ChangeDTO<>(State.Fail, "Error removing book from wishlist: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> clearWishlist(Integer userId) {
        try {
            log.info("Clearing wishlist for user {}", userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for clearing wishlist");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<BookCollection> wishlistOpt = getWishlistByUserId(userId);
            if (wishlistOpt.isEmpty()) {
                log.warn("User {} does not have a wishlist", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User does not have a wishlist", null);
            }

            BookCollection wishlist = wishlistOpt.get();

            List<BooksBookCollections> booksInWishlist = booksBookCollectionsRepository
                    .findByBookCollection_BcolsId(wishlist.getBcolsId());

            int booksCount = booksInWishlist.size();

            if (!booksInWishlist.isEmpty()) {
                booksBookCollectionsRepository.deleteAll(booksInWishlist);
                log.info("Cleared {} books from wishlist for user {}", booksCount, userId);
            } else {
                log.info("Wishlist is already empty for user {}", userId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("wishlistId", wishlist.getBcolsId());
            response.put("booksRemoved", booksCount);
            response.put("cleared", true);

            return new ChangeDTO<>(State.OK, "Wishlist cleared successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when clearing wishlist: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error clearing wishlist: ", e);
            return new ChangeDTO<>(State.Fail, "Error clearing wishlist: " + e.getMessage(), null);
        }
    }

    private CollectionPrivilegeDTO convertToCollectionPrivilegeDTO(CollectionViewPrivilege privilege) {
        CollectionPrivilegeDTO dto = new CollectionPrivilegeDTO();
        dto.setCvpId(privilege.getCvpId());
        dto.setCollectionId(privilege.getBcolsId());
        dto.setUserId(privilege.getUserId());
        dto.setStatus(String.valueOf(privilege.getStatus()));

        Optional<User> userOpt = userRepository.findById(privilege.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            dto.setUsername(user.getUsername());

            List<User> usersWithProfiles = userRepository.findByIdsWithProfiles(List.of(user.getUserId()));
            if (!usersWithProfiles.isEmpty() && usersWithProfiles.get(0).getProfile() != null) {
                dto.setNickname(usersWithProfiles.get(0).getProfile().getNickname());
            }
        }

        return dto;
    }

    private boolean hasCollectionAccess(BookCollection collection, Integer userId) {
        if (userId == null) {
            return true;
        }
        if (userId != null && userId == -1) {
            return false;
        }
        return collection.getOwner() != null &&
                collection.getOwner().getUserId().equals(userId);
    }

    private Boolean checkCollectionAccess(Integer collectionId, Integer userId) {
        try {
            Optional<BookCollection> collectionOpt = bookCollectionRepository.findById(collectionId);
            if (collectionOpt.isEmpty()) {
                return false;
            }

            BookCollection collection = collectionOpt.get();

            if (userId == null) {
                return true;
            }

            if (userId != null && userId == -1) {
                return "Public".equalsIgnoreCase(String.valueOf(collection.getConfidentiality()));
            }

            if ("Public".equalsIgnoreCase(String.valueOf(collection.getConfidentiality()))) {
                return true;
            }

            if (collection.getOwner() != null && collection.getOwner().getUserId().equals(userId)) {
                return true;
            }

            return collectionAccessRepository.canViewCollection(userId, collectionId);

        } catch (Exception e) {
            log.error("Error checking collection access: ", e);
            return false;
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
        } else if (message.contains("unique constraint") && message.contains("book_reviews_user_id_book_id_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "You have already reviewed this book", null);
        } else if (message.contains("unique constraint") && message.contains("liked_collections_user_id_bcols_id_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "You have already liked this collection", null);
        } else if (message.contains("unique constraint") && message.contains("books_book_collections_book_id_bcols_id_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Book already exists in collection", null);
        } else {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Data integrity violation: " + message, null);
        }
    }

    private Page<BooksBookCollections> getBooksInCollectionPage(Integer collectionId, Pageable pageable) {
        return booksBookCollectionsRepository.findByBookCollection_BcolsId(collectionId, pageable);
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> checkBookInWishlist(Integer userId, Integer bookId) {
        try {
            log.debug("Checking if book {} exists in wishlist for user {}", bookId, userId);

            if (userId == null || userId == -1) {
                log.warn("User ID is null or -1 for checking book in wishlist");
                return new ChangeDTO<>(State.Fail_Forbidden, "User ID is required and must be registered", null);
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            Optional<BookCollection> wishlistOpt = getWishlistByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("bookId", bookId);

            if (wishlistOpt.isEmpty()) {
                response.put("hasWishlist", false);
                response.put("existsInWishlist", false);
                response.put("message", "User does not have a wishlist");

                log.debug("User {} does not have a wishlist", userId);
                return new ChangeDTO<>(State.OK, "User does not have a wishlist", response);
            }

            BookCollection wishlist = wishlistOpt.get();
            response.put("hasWishlist", true);
            response.put("wishlistId", wishlist.getBcolsId());
            response.put("wishlistTitle", wishlist.getTitle());
            response.put("confidentiality", wishlist.getConfidentiality());

            List<BooksBookCollections> booksInWishlist = booksBookCollectionsRepository
                    .findByBook_BookIdIn(List.of(bookId));

            boolean exists = booksInWishlist.stream()
                    .anyMatch(bbc -> bbc.getBookCollection().getBcolsId().equals(wishlist.getBcolsId()));

            response.put("existsInWishlist", exists);

            if (exists) {
                Optional<BooksBookCollections> bbcOpt = booksInWishlist.stream()
                        .filter(bbc -> bbc.getBookCollection().getBcolsId().equals(wishlist.getBcolsId()))
                        .findFirst();

                if (bbcOpt.isPresent()) {
                    BooksBookCollections bbc = bbcOpt.get();
                    response.put("bookCollectionId", bbc.getCBookBcolId());
                }

                Book book = bookOpt.get();
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("title", book.getTitle());
                bookInfo.put("subtitle", book.getSubtitle());
                bookInfo.put("isbn", book.getIsbn());
                bookInfo.put("pageCnt", book.getPageCnt());

                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    List<Map<String, Object>> authorsInfo = book.getAuthors().stream()
                            .map(author -> {
                                Map<String, Object> authorInfo = new HashMap<>();
                                authorInfo.put("authorId", author.getAuthorId());
                                authorInfo.put("name", author.getName());
                                authorInfo.put("realName", author.getRealName());
                                return authorInfo;
                            })
                            .collect(Collectors.toList());
                    bookInfo.put("authors", authorsInfo);
                }

                if (book.getGenres() != null && !book.getGenres().isEmpty()) {
                    List<Map<String, Object>> genresInfo = book.getGenres().stream()
                            .map(genre -> {
                                Map<String, Object> genreInfo = new HashMap<>();
                                genreInfo.put("genreId", genre.getGenreId());
                                genreInfo.put("name", genre.getName());
                                return genreInfo;
                            })
                            .collect(Collectors.toList());
                    bookInfo.put("genres", genresInfo);
                }

                response.put("bookInfo", bookInfo);

                if (book.getPhotoLink() != null) {
                    ImageLink photoLink = book.getPhotoLink();
                    Integer imageLinkId = photoLink.getImglId();
                    List<ImageLink> imageLinks = imageLinkRepository.findByIdsWithImageData(List.of(imageLinkId));
                    if (!imageLinks.isEmpty()) {
                        ImageLink fullImageLink = imageLinks.get(0);
                        if (fullImageLink.getImageData() != null) {
                            ImageData imageData = fullImageLink.getImageData();
                            ImageDataDTO imageDataDTO = new ImageDataDTO(
                                    imageData.getImgdId(),
                                    imageData.getUuid(),
                                    imageData.getSize(),
                                    imageData.getMimeType(),
                                    imageData.getExtension()
                            );
                            ImageLinkDTO imageLinkDTO = new ImageLinkDTO(fullImageLink.getImglId(), imageDataDTO);
                            response.put("bookCover", imageLinkDTO);
                        }
                    }
                }
            }

            Long booksCount = booksBookCollectionsRepository.countByBookCollection_BcolsId(wishlist.getBcolsId());
            response.put("wishlistBooksCount", booksCount);

            List<Object[]> likesResults = likedCollectionRepository.findPopularCollections();
            Long likesCount = likesResults.stream()
                    .filter(row -> wishlist.getBcolsId().equals((Integer) row[0]))
                    .map(row -> (Long) row[1])
                    .findFirst()
                    .orElse(0L);
            response.put("wishlistLikesCount", likesCount);

            log.debug("Book {} exists in wishlist for user {}: {}", bookId, userId, exists);
            return new ChangeDTO<>(State.OK,
                    exists ? "Book exists in user's wishlist" : "Book does not exist in user's wishlist",
                    response);

        } catch (Exception e) {
            log.error("Error checking if book exists in wishlist: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error checking if book exists in wishlist: " + e.getMessage(), null);
        }
    }
}