package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.*;
import com.fuzis.booksbackend.repository.*;
import com.fuzis.booksbackend.transfer.*;
import com.fuzis.booksbackend.transfer.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularService {

    private final SubscriberRepository subscriberRepository;
    private final LikedCollectionRepository likedCollectionRepository;
    private final BooksBookCollectionsRepository booksBookCollectionsRepository;
    private final BookReviewRepository bookReviewRepository;
    private final UserRepository userRepository;
    private final BookCollectionRepository bookCollectionRepository;
    private final BookRepository bookRepository;
    private final ImageLinkRepository imageLinkRepository;

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getPopularUsers(Integer limit) {
        try {
            log.debug("Getting {} most popular users", limit);

            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Object[]> popularUserResults = subscriberRepository.findPopularUsers();

            List<Object[]> limitedResults = popularUserResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<Integer> userIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            List<User> users = userRepository.findByIdsWithProfiles(userIds);

            List<Integer> imageIds = users.stream()
                    .map(u -> u.getProfile() != null ? u.getProfile().getUserImglId() : null)
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

            Map<Integer, Long> subscribersCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<PopularUserDTO> popularUsers = users.stream()
                    .map(user -> {
                        PopularUserDTO dto = new PopularUserDTO();
                        dto.setUserId(user.getUserId());
                        dto.setUsername(user.getUsername());

                        if (user.getProfile() != null) {
                            dto.setNickname(user.getProfile().getNickname());
                            dto.setEmail(user.getProfile().getEmail());
                            dto.setStatus(user.getProfile().getStatus());

                            ImageLink profileImageLink = user.getProfile().getUserImglId();
                            if (profileImageLink != null) {
                                ImageLink fullImageLink = imageLinksMap.get(profileImageLink.getImglId());
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
                                    dto.setProfileImage(imageLinkDTO);
                                }
                            }
                        }

                        dto.setSubscribersCount(subscribersCountMap.getOrDefault(user.getUserId(), 0L));
                        return dto;
                    })
                    .collect(Collectors.toList());

            popularUsers.sort((a, b) -> Long.compare(b.getSubscribersCount(), a.getSubscribersCount()));

            Map<String, Object> response = new HashMap<>();
            response.put("users", popularUsers);
            response.put("limit", limit);
            response.put("count", popularUsers.size());

            if (popularUsers.isEmpty()) {
                log.debug("No popular users found");
                return new ChangeDTO<>(State.OK,
                        "No popular users found", response);
            }

            log.debug("Found {} popular users", popularUsers.size());
            return new ChangeDTO<>(State.OK,
                    "Popular users retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving popular users: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving popular users: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getPopularCollections(Integer limit) {
        try {
            log.debug("Getting {} most popular collections", limit);

            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Object[]> popularCollectionResults = likedCollectionRepository.findPopularCollections();

            List<Object[]> limitedResults = popularCollectionResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<Integer> collectionIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            List<BookCollection> collections = bookCollectionRepository.findByIdsWithPhotoLinks(collectionIds);

            List<Object[]> bookCountsResults = booksBookCollectionsRepository.findBookCountsByCollectionIds(collectionIds);
            Map<Integer, Long> bookCountsMap = bookCountsResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<Integer> ownerIds = collections.stream()
                    .map(BookCollection::getOwner)
                    .filter(Objects::nonNull)
                    .map(User::getUserId)
                    .collect(Collectors.toList());

            List<User> owners = userRepository.findByIdsWithProfiles(ownerIds);
            Map<Integer, User> ownersMap = owners.stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));

            List<Integer> imageIds = collections.stream()
                    .map(BookCollection::getPhotoLink)
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

            Map<Integer, Long> likesCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<PopularCollectionDTO> popularCollections = collections.stream()
                    .map(collection -> {
                        PopularCollectionDTO dto = new PopularCollectionDTO();
                        dto.setCollectionId(collection.getBcolsId());
                        dto.setTitle(collection.getTitle());
                        dto.setDescription(collection.getDescription());
                        dto.setCollectionType(collection.getCollectionType().name());

                        User owner = collection.getOwner();
                        if (owner != null) {
                            dto.setOwnerId(owner.getUserId());
                            User fullOwner = ownersMap.get(owner.getUserId());
                            if (fullOwner != null && fullOwner.getProfile() != null) {
                                dto.setOwnerNickname(fullOwner.getProfile().getNickname());
                            }
                        }

                        dto.setBookCount(bookCountsMap.getOrDefault(collection.getBcolsId(), 0L).intValue());

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

                        dto.setLikesCount(likesCountMap.getOrDefault(collection.getBcolsId(), 0L));
                        return dto;
                    })
                    .collect(Collectors.toList());

            popularCollections.sort((a, b) -> Long.compare(b.getLikesCount(), a.getLikesCount()));

            Map<String, Object> response = new HashMap<>();
            response.put("collections", popularCollections);
            response.put("limit", limit);
            response.put("count", popularCollections.size());

            if (popularCollections.isEmpty()) {
                log.debug("No popular collections found");
                return new ChangeDTO<>(State.OK,
                        "No popular collections found", response);
            }

            log.debug("Found {} popular collections", popularCollections.size());
            return new ChangeDTO<>(State.OK,
                    "Popular collections retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving popular collections: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving popular collections: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getPopularBooks(Integer limit) {
        try {
            log.debug("Getting {} most popular books", limit);

            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Object[]> popularBookResults = booksBookCollectionsRepository.findPopularBooks();

            List<Object[]> limitedResults = popularBookResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<Integer> bookIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
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

            Map<Integer, Long> collectionsCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<PopularBookDTO> popularBooks = books.stream()
                    .map(book -> {
                        PopularBookDTO dto = new PopularBookDTO();
                        dto.setBookId(book.getBookId());
                        dto.setTitle(book.getTitle());
                        dto.setSubtitle(book.getSubtitle());
                        dto.setIsbn(book.getIsbn());
                        dto.setPageCnt(book.getPageCnt());

                        Double avgRating = averageRatingsMap.get(book.getBookId());
                        if (avgRating != null) {
                            dto.setAverageRating(Math.round(avgRating * 100.0) / 100.0);
                        } else {
                            dto.setAverageRating(null);
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

                        dto.setCollectionsCount(collectionsCountMap.getOrDefault(book.getBookId(), 0L));
                        return dto;
                    })
                    .collect(Collectors.toList());

            popularBooks.sort((a, b) -> Long.compare(b.getCollectionsCount(), a.getCollectionsCount()));

            Map<String, Object> response = new HashMap<>();
            response.put("books", popularBooks);
            response.put("limit", limit);
            response.put("count", popularBooks.size());

            if (popularBooks.isEmpty()) {
                log.debug("No popular books found");
                return new ChangeDTO<>(State.OK,
                        "No popular books found", response);
            }

            log.debug("Found {} popular books", popularBooks.size());
            return new ChangeDTO<>(State.OK,
                    "Popular books retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving popular books: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving popular books: " + e.getMessage(), null);
        }
    }
}