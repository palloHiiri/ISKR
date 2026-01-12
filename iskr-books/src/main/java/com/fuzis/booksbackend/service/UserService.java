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
public class UserService {

    private final UserRepository userRepository;
    private final SubscriberRepository subscriberRepository;
    private final BookCollectionRepository bookCollectionRepository;
    private final BooksBookCollectionsRepository booksBookCollectionsRepository;
    private final ImageLinkRepository imageLinkRepository;

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getUserDetail(Integer userId) {
        try {
            log.debug("Getting user details for ID: {}", userId);

            List<User> users = userRepository.findByIdsWithProfiles(List.of(userId));
            if (users.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }
            User user = users.get(0);

            ImageLinkDTO profileImageDTO = null;
            if (user.getProfile() != null && user.getProfile().getUserImglId() != null) {
                Integer imageLinkId = user.getProfile().getUserImglId().getImglId();
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
                        profileImageDTO = new ImageLinkDTO(imageLink.getImglId(), imageDataDTO);
                    }
                }
            }

            long subscribersCount = subscriberRepository.countBySubsUserOn_UserId(userId);
            long subscriptionsCount = subscriberRepository.countBySubsUser_UserId(userId);

            long collectionsCount = bookCollectionRepository.countByOwner_UserId(userId);

            UserDetailDTO dto = new UserDetailDTO();
            dto.setUserId(user.getUserId());
            dto.setUsername(user.getUsername());
            dto.setRegisteredDate(user.getRegisteredDate());

            if (user.getProfile() != null) {
                dto.setNickname(user.getProfile().getNickname());
                dto.setEmail(user.getProfile().getEmail());
                dto.setProfileDescription(user.getProfile().getProfileDescription());
                dto.setBirthDate(user.getProfile().getBirthDate());
                dto.setEmailVerified(user.getProfile().getEmailVerified());
                dto.setStatus(user.getProfile().getStatus());
            }

            dto.setProfileImage(profileImageDTO);
            dto.setSubscribersCount(subscribersCount);
            dto.setSubscriptionsCount(subscriptionsCount);
            dto.setCollectionsCount(collectionsCount); 

            log.debug("User details retrieved for ID: {}, collections count: {}", userId, collectionsCount);
            return new ChangeDTO<>(State.OK, "User details retrieved successfully", dto);

        } catch (Exception e) {
            log.error("Error retrieving user details for ID {}: ", userId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving user details: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getUserSubscribers(Integer userId, Integer page, Integer batch) {
        try {
            log.debug("Getting subscribers for user {}, page: {}, batch: {}", userId, page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<Subscriber> subscribersPage = subscriberRepository.findBySubsUserOn_UserId(userId, pageable);

            List<User> subscribers = subscribersPage.getContent().stream()
                    .map(Subscriber::getSubsUser)
                    .collect(Collectors.toList());

            List<UserSubscriptionDTO> subscriberDTOs = convertUsersToSubscriptionDTOs(subscribers);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", subscribersPage.getTotalPages());
            response.put("totalElements", subscribersPage.getTotalElements());
            response.put("subscribers", subscriberDTOs);

            if (subscriberDTOs.isEmpty()) {
                log.debug("No subscribers found for user {}", userId);
                return new ChangeDTO<>(State.OK, "No subscribers found", response);
            }

            log.debug("Found {} subscribers for user {}", subscriberDTOs.size(), userId);
            return new ChangeDTO<>(State.OK, "Subscribers retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving subscribers for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving subscribers: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getUserSubscriptions(Integer userId, Integer page, Integer batch) {
        try {
            log.debug("Getting subscriptions for user {}, page: {}, batch: {}", userId, page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);
            Page<Subscriber> subscriptionsPage = subscriberRepository.findBySubsUser_UserId(userId, pageable);

            List<User> subscriptions = subscriptionsPage.getContent().stream()
                    .map(Subscriber::getSubsUserOn)
                    .collect(Collectors.toList());

            List<UserSubscriptionDTO> subscriptionDTOs = convertUsersToSubscriptionDTOs(subscriptions);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", subscriptionsPage.getTotalPages());
            response.put("totalElements", subscriptionsPage.getTotalElements());
            response.put("subscriptions", subscriptionDTOs);

            if (subscriptionDTOs.isEmpty()) {
                log.debug("No subscriptions found for user {}", userId);
                return new ChangeDTO<>(State.OK, "No subscriptions found", response);
            }

            log.debug("Found {} subscriptions for user {}", subscriptionDTOs.size(), userId);
            return new ChangeDTO<>(State.OK, "Subscriptions retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving subscriptions for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving subscriptions: " + e.getMessage(), null);
        }
    }

    private List<UserSubscriptionDTO> convertUsersToSubscriptionDTOs(List<User> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());

        List<User> usersWithProfiles = userRepository.findByIdsWithProfiles(userIds);

        List<Integer> imageIds = usersWithProfiles.stream()
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

        Map<Integer, Long> subscribersCountMap = getSubscribersCountForUsers(userIds);

        return usersWithProfiles.stream()
                .map(user -> {
                    UserSubscriptionDTO dto = new UserSubscriptionDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());

                    if (user.getProfile() != null) {
                        dto.setNickname(user.getProfile().getNickname());

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
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getUserCollections(Integer userId, Integer page, Integer batch) {
        try {
            log.debug("Getting collections for user {}, page: {}, batch: {}", userId, page, batch);

            if (page == null || page < 0) {
                page = 0;
            }
            if (batch == null || batch <= 0) {
                batch = 10;
            }

            Pageable pageable = PageRequest.of(page, batch);

            Page<BookCollection> collectionsPage = bookCollectionRepository.findByOwner_UserId(userId, pageable);

            List<Integer> collectionIds = collectionsPage.getContent().stream()
                    .map(BookCollection::getBcolsId)
                    .collect(Collectors.toList());

            List<Object[]> bookCountsResults = booksBookCollectionsRepository.findBookCountsByCollectionIds(collectionIds);
            Map<Integer, Long> bookCountsMap = bookCountsResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Integer) row[0],
                            row -> (Long) row[1]
                    ));

            List<Integer> imageIds = collectionsPage.getContent().stream()
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

            List<UserCollectionDTO> collectionDTOs = collectionsPage.getContent().stream()
                    .map(collection -> {
                        UserCollectionDTO dto = new UserCollectionDTO();
                        dto.setCollectionId(collection.getBcolsId());
                        dto.setTitle(collection.getTitle());
                        dto.setDescription(collection.getDescription());
                        dto.setConfidentiality(collection.getConfidentiality().name());
                        dto.setCollectionType(collection.getCollectionType().name());

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

                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", collectionsPage.getTotalPages());
            response.put("totalElements", collectionsPage.getTotalElements());
            response.put("collections", collectionDTOs);

            if (collectionDTOs.isEmpty()) {
                log.debug("No collections found for user {}", userId);
                return new ChangeDTO<>(State.OK, "No collections found", response);
            }

            log.debug("Found {} collections for user {}", collectionDTOs.size(), userId);
            return new ChangeDTO<>(State.OK, "Collections retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving collections for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail, "Error retrieving collections: " + e.getMessage(), null);
        }
    }

    private Map<Integer, Long> getSubscribersCountForUsers(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = subscriberRepository.findSubscribersCountByUserIds(userIds);

        Map<Integer, Long> subscribersCountMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Long) row[1]
                ));

        userIds.forEach(userId -> subscribersCountMap.putIfAbsent(userId, 0L));

        return subscribersCountMap;
    }
}