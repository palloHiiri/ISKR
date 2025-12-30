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

            // Получаем популярных пользователей по количеству подписчиков
            List<Object[]> popularUserResults = subscriberRepository.findPopularUsers();

            // Ограничиваем количество
            List<Object[]> limitedResults = popularUserResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // Извлекаем ID пользователей
            List<Integer> userIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            // Получаем пользователей с профилями
            List<User> users = userRepository.findByIdsWithProfiles(userIds);

            // Получаем ID изображений
            List<Integer> imageIds = users.stream()
                    .map(u -> u.getProfile() != null ? u.getProfile().getUserImglId() : null)
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

            // Создаем Map для быстрого доступа к количеству подписчиков
            Map<Integer, Long> subscribersCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            // Преобразуем в DTO
            List<PopularUserDTO> popularUsers = users.stream()
                    .map(user -> {
                        PopularUserDTO dto = new PopularUserDTO();
                        dto.setUserId(user.getUserId());
                        dto.setUsername(user.getUsername());

                        if (user.getProfile() != null) {
                            dto.setNickname(user.getProfile().getNickname());
                            dto.setEmail(user.getProfile().getEmail());
                            dto.setStatus(user.getProfile().getStatus());

                            // Добавляем изображение профиля
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

            // Сортируем по количеству подписчиков (убывающе)
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

            // Получаем популярные коллекции по количеству лайков
            List<Object[]> popularCollectionResults = likedCollectionRepository.findPopularCollections();

            // Ограничиваем количество
            List<Object[]> limitedResults = popularCollectionResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // Извлекаем ID коллекций
            List<Integer> collectionIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            // Получаем коллекции с фотографиями
            List<BookCollection> collections = bookCollectionRepository.findByIdsWithPhotoLinks(collectionIds);

            // Получаем ID владельцев
            List<Integer> ownerIds = collections.stream()
                    .map(BookCollection::getOwner)
                    .filter(Objects::nonNull)
                    .map(User::getUserId)
                    .collect(Collectors.toList());

            // Получаем владельцев с профилями
            List<User> owners = userRepository.findByIdsWithProfiles(ownerIds);
            Map<Integer, User> ownersMap = owners.stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));

            // Получаем ID изображений
            List<Integer> imageIds = collections.stream()
                    .map(BookCollection::getPhotoLink)
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

            // Создаем Map для быстрого доступа к количеству лайков
            Map<Integer, Long> likesCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            // Преобразуем в DTO
            List<PopularCollectionDTO> popularCollections = collections.stream()
                    .map(collection -> {
                        PopularCollectionDTO dto = new PopularCollectionDTO();
                        dto.setCollectionId(collection.getBcolsId());
                        dto.setTitle(collection.getTitle());
                        dto.setDescription(collection.getDescription());
                        dto.setCollectionType(collection.getCollectionType().name());

                        // Добавляем информацию о владельце
                        User owner = collection.getOwner();
                        if (owner != null) {
                            dto.setOwnerId(owner.getUserId());
                            User fullOwner = ownersMap.get(owner.getUserId());
                            if (fullOwner != null && fullOwner.getProfile() != null) {
                                dto.setOwnerNickname(fullOwner.getProfile().getNickname());
                            }
                        }

                        // Добавляем изображение коллекции
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

            // Сортируем по количеству лайков (убывающе)
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

            // Получаем популярные книги по количеству добавлений в коллекции
            List<Object[]> popularBookResults = booksBookCollectionsRepository.findPopularBooks();

            // Ограничиваем количество
            List<Object[]> limitedResults = popularBookResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // Извлекаем ID книг
            List<Integer> bookIds = limitedResults.stream()
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            // Получаем книги с авторами и жанрами
            List<Book> books = bookRepository.findAllById(bookIds);

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

            // Создаем Map для быстрого доступа к количеству коллекций
            Map<Integer, Long> collectionsCountMap = limitedResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            // Преобразуем в DTO
            List<PopularBookDTO> popularBooks = books.stream()
                    .map(book -> {
                        PopularBookDTO dto = new PopularBookDTO();
                        dto.setBookId(book.getBookId());
                        dto.setTitle(book.getTitle());
                        dto.setSubtitle(book.getSubtitle());
                        dto.setIsbn(book.getIsbn());
                        dto.setPageCnt(book.getPageCnt());

                        // Добавляем изображение книги
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

            // Сортируем по количеству коллекций (убывающе)
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