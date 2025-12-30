package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.Subscriber;
import com.fuzis.booksbackend.entity.User;
import com.fuzis.booksbackend.repository.SubscriberRepository;
import com.fuzis.booksbackend.repository.UserRepository;
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
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChangeDTO<Object> subscribe(Integer userId, Integer userOnId) {
        try {
            log.info("User {} subscribing to user {}", userId, userOnId);

            // Check if user is trying to subscribe to themselves
            if (userId.equals(userOnId)) {
                log.warn("User {} attempted to subscribe to themselves", userId);
                return new ChangeDTO<>(State.Fail_BadData,
                        "Cannot subscribe to yourself", null);
            }

            // Check if users exist
            Optional<User> subsUserOpt = userRepository.findById(userId);
            Optional<User> subsUserOnOpt = userRepository.findById(userOnId);

            if (subsUserOpt.isEmpty() || subsUserOnOpt.isEmpty()) {
                log.warn("One or both users not found: user {} -> user {}", userId, userOnId);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "One or both users not found", null);
            }

            // Check if subscription already exists
            if (subscriberRepository.existsBySubsUserAndSubsUserOn(subsUserOpt.get(), subsUserOnOpt.get())) {
                log.warn("Subscription already exists: user {} -> user {}", userId, userOnId);
                return new ChangeDTO<>(State.Fail_Conflict,
                        "Already subscribed to this user", null);
            }

            // Create subscription
            Subscriber subscriber = Subscriber.builder()
                    .subsUser(subsUserOpt.get())
                    .subsUserOn(subsUserOnOpt.get())
                    .build();

            Subscriber savedSubscriber = subscriberRepository.save(subscriber);
            log.info("Subscription created successfully: user {} -> user {}", userId, userOnId);

            return new ChangeDTO<>(State.OK,
                    "Successfully subscribed", savedSubscriber);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when subscribing: ", e);
            return handleDataIntegrityViolation(e, userId, userOnId);
        } catch (Exception e) {
            log.error("Error creating subscription: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error creating subscription: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> unsubscribe(Integer userId, Integer userOnId) {
        try {
            log.info("User {} unsubscribing from user {}", userId, userOnId);

            // Check if users exist
            Optional<User> subsUserOpt = userRepository.findById(userId);
            Optional<User> subsUserOnOpt = userRepository.findById(userOnId);

            if (subsUserOpt.isEmpty() || subsUserOnOpt.isEmpty()) {
                log.warn("One or both users not found: user {} -> user {}", userId, userOnId);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "One or both users not found", null);
            }

            // Check if subscription exists
            if (!subscriberRepository.existsBySubsUserAndSubsUserOn(subsUserOpt.get(), subsUserOnOpt.get())) {
                log.warn("Subscription not found: user {} -> user {}", userId, userOnId);
                return new ChangeDTO<>(State.Fail_NotFound,
                        "Subscription not found", null);
            }

            // Delete subscription
            subscriberRepository.deleteBySubsUserAndSubsUserOn(subsUserOpt.get(), subsUserOnOpt.get());
            log.info("Subscription deleted successfully: user {} -> user {}", userId, userOnId);

            return new ChangeDTO<>(State.OK,
                    "Successfully unsubscribed", null);

        } catch (Exception e) {
            log.error("Error deleting subscription: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error deleting subscription: " + e.getMessage(), null);
        }
    }

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

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", subscriptionsPage.getTotalPages());
            response.put("totalElements", subscriptionsPage.getTotalElements());
            response.put("subscriptions", subscriptionsPage.getContent());

            if (subscriptionsPage.isEmpty()) {
                log.debug("No subscriptions found for user {}", userId);
                return new ChangeDTO<>(State.OK,
                        "No subscriptions found", response);
            }

            log.debug("Found {} subscriptions for user {}", subscriptionsPage.getNumberOfElements(), userId);
            return new ChangeDTO<>(State.OK,
                    "Subscriptions retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving subscriptions for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving subscriptions: " + e.getMessage(), null);
        }
    }

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

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("page", page);
            response.put("batch", batch);
            response.put("totalPages", subscribersPage.getTotalPages());
            response.put("totalElements", subscribersPage.getTotalElements());
            response.put("subscribers", subscribersPage.getContent());

            if (subscribersPage.isEmpty()) {
                log.debug("No subscribers found for user {}", userId);
                return new ChangeDTO<>(State.OK,
                        "No subscribers found", response);
            }

            log.debug("Found {} subscribers for user {}", subscribersPage.getNumberOfElements(), userId);
            return new ChangeDTO<>(State.OK,
                    "Subscribers retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error retrieving subscribers for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail,
                    "Error retrieving subscribers: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> isSubscriber(Integer userId, Integer userOnId) {
        try {
            log.debug("Checking if user {} is subscribed to user {}", userId, userOnId);

            Optional<User> subsUserOpt = userRepository.findById(userId);
            Optional<User> subsUserOnOpt = userRepository.findById(userOnId);

            if (subsUserOpt.isEmpty() || subsUserOnOpt.isEmpty()) {
                return new ChangeDTO<>(State.Fail_NotFound,
                        "One or both users not found", null);
            }

            boolean isSubscribed = subscriberRepository.existsBySubsUserAndSubsUserOn(
                    subsUserOpt.get(), subsUserOnOpt.get());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("userOnId", userOnId);
            response.put("isSubscriber", isSubscribed);

            if (isSubscribed) {
                log.debug("User {} is subscribed to user {}", userId, userOnId);
                return new ChangeDTO<>(State.OK,
                        "User is subscribed to the specified user", response);
            } else {
                log.debug("User {} is not subscribed to user {}", userId, userOnId);
                return new ChangeDTO<>(State.OK,
                        "User is not subscribed to the specified user", response);
            }

        } catch (Exception e) {
            log.error("Error checking subscription status: ", e);
            return new ChangeDTO<>(State.Fail,
                    "Error checking subscription status: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> countSubscribers(Integer userId) {
        try {
            log.debug("Counting subscribers for user {}", userId);

            long count = subscriberRepository.countBySubsUserOn_UserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("subscribersCount", count);

            log.debug("User {} has {} subscribers", userId, count);
            return new ChangeDTO<>(State.OK,
                    "Subscribers count retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error counting subscribers for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail,
                    "Error counting subscribers: " + e.getMessage(), null);
        }
    }

    public ChangeDTO<Object> countSubscriptions(Integer userId) {
        try {
            log.debug("Counting subscriptions for user {}", userId);

            long count = subscriberRepository.countBySubsUser_UserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("subscriptionsCount", count);

            log.debug("User {} has {} subscriptions", userId, count);
            return new ChangeDTO<>(State.OK,
                    "Subscriptions count retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error counting subscriptions for user {}: ", userId, e);
            return new ChangeDTO<>(State.Fail,
                    "Error counting subscriptions: " + e.getMessage(), null);
        }
    }

    private ChangeDTO<Object> handleDataIntegrityViolation(DataIntegrityViolationException e, Integer userId, Integer userOnId) {
        String message = e.getMostSpecificCause().getMessage();

        if (message.contains("subs_user_id")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "User with ID " + userId + " does not exist", null);
        } else if (message.contains("subs_user_on_id")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "User with ID " + userOnId + " does not exist", null);
        } else {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Data integrity violation: " + message, null);
        }
    }
}