package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.*;
import com.fuzis.booksbackend.repository.*;
import com.fuzis.booksbackend.transfer.*;
import com.fuzis.booksbackend.transfer.state.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingService {

    private final BookReadingStatusRepository bookReadingStatusRepository;
    private final ReadingGoalRepository readingGoalRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookReviewRepository bookReviewRepository;

    @Transactional
    public ChangeDTO<Object> createBookReadingStatus(Integer userId, BookReadingStatusRequestDTO dto) {
        try {
            log.info("Creating reading status for user {} and book {}", userId, dto.getBookId());

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            Optional<Book> bookOpt = bookRepository.findById(dto.getBookId());
            if (bookOpt.isEmpty()) {
                log.warn("Book not found with ID: {}", dto.getBookId());
                return new ChangeDTO<>(State.Fail_NotFound, "Book not found", null);
            }

            if (bookReadingStatusRepository.existsByUserIdAndBook_BookId(userId, dto.getBookId())) {
                log.warn("Reading status already exists for user {} and book {}", userId, dto.getBookId());
                return new ChangeDTO<>(State.Fail_Conflict, "Reading status already exists for this book", null);
            }

            List<String> validStatuses = Arrays.asList("Planning", "Reading", "Delayed", "GaveUp", "Finished");
            if (!validStatuses.contains(dto.getReadingStatus())) {
                log.warn("Invalid reading status: {}", dto.getReadingStatus());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid reading status. Must be one of: Planning, Reading, Delayed, GaveUp, Finished", null);
            }

            BookReadingStatus status = BookReadingStatus.builder()
                    .userId(userId)
                    .book(bookOpt.get())
                    .readingStatus(dto.getReadingStatus())
                    .pageRead(0)
                    .lastReadDate(null)
                    .build();

            BookReadingStatus savedStatus = bookReadingStatusRepository.save(status);
            log.info("Reading status created with ID: {}", savedStatus.getBrsId());

            BookReadingStatusResponseDTO response = convertToBookReadingStatusResponseDTO(savedStatus);
            return new ChangeDTO<>(State.OK, "Reading status created successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating reading status: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error creating reading status: ", e);
            return new ChangeDTO<>(State.Fail, "Error creating reading status: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateBookReadingStatus(Integer userId, Integer bookId, UpdateReadingStatusRequestDTO dto) {
        try {
            log.info("Updating reading status for user {} and book {}", userId, bookId);

            Optional<BookReadingStatus> statusOpt = bookReadingStatusRepository
                    .findByUserIdAndBook_BookId(userId, bookId);

            if (statusOpt.isEmpty()) {
                log.warn("Reading status not found for user {} and book {}", userId, bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Reading status not found", null);
            }

            BookReadingStatus status = statusOpt.get();

            List<String> validStatuses = Arrays.asList("Planning", "Reading", "Delayed", "GaveUp", "Finished");
            if (!validStatuses.contains(dto.getReadingStatus())) {
                log.warn("Invalid reading status: {}", dto.getReadingStatus());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid reading status. Must be one of: Planning, Reading, Delayed, GaveUp, Finished", null);
            }

            status.setReadingStatus(dto.getReadingStatus());

            if ("Finished".equals(dto.getReadingStatus())) {
                Book book = status.getBook();
                if (status.getPageRead() < book.getPageCnt()) {
                    log.warn("Cannot set status to Finished: not all pages are read. Page read: {}, Total pages: {}",
                            status.getPageRead(), book.getPageCnt());
                    return new ChangeDTO<>(State.Fail_BadData,
                            "Cannot set status to Finished: not all pages are read", null);
                }
            }

            if ("Reading".equals(dto.getReadingStatus()) || "Finished".equals(dto.getReadingStatus())) {
                status.setLastReadDate(LocalDateTime.now());
            }

            BookReadingStatus updatedStatus = bookReadingStatusRepository.save(status);
            log.info("Reading status updated for user {} and book {}", userId, bookId);

            BookReadingStatusResponseDTO response = convertToBookReadingStatusResponseDTO(updatedStatus);
            return new ChangeDTO<>(State.OK, "Reading status updated successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating reading status: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating reading status: ", e);
            return new ChangeDTO<>(State.Fail, "Error updating reading status: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getBookReadingStatus(Integer userId, Integer bookId) {
        try {
            log.debug("Getting reading status for user {} and book {}", userId, bookId);

            Optional<BookReadingStatus> statusOpt = bookReadingStatusRepository
                    .findByUserIdAndBook_BookId(userId, bookId);

            if (statusOpt.isEmpty()) {
                log.debug("Reading status not found for user {} and book {}", userId, bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Reading status not found", null);
            }

            BookReadingStatusResponseDTO response = convertToBookReadingStatusResponseDTO(statusOpt.get());
            return new ChangeDTO<>(State.OK, "Reading status retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error getting reading status: ", e);
            return new ChangeDTO<>(State.Fail, "Error getting reading status: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateReadingProgress(Integer userId, Integer bookId, UpdateReadingProgressRequestDTO dto) {
        try {
            log.info("Updating reading progress for user {} and book {}: +{} pages",
                    userId, bookId, dto.getPageRead());

            Optional<BookReadingStatus> statusOpt = bookReadingStatusRepository
                    .findByUserIdAndBook_BookId(userId, bookId);

            if (statusOpt.isEmpty()) {
                log.warn("Reading status not found for user {} and book {}", userId, bookId);
                return new ChangeDTO<>(State.Fail_NotFound, "Reading status not found", null);
            }

            BookReadingStatus status = statusOpt.get();
            Book book = status.getBook();

            int newPageRead = status.getPageRead() + dto.getPageRead();
            if (newPageRead > book.getPageCnt()) {
                log.warn("Page read exceeds total pages. Current: {}, Adding: {}, Total: {}",
                        status.getPageRead(), dto.getPageRead(), book.getPageCnt());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Page read cannot exceed total pages in the book", null);
            }

            status.setPageRead(newPageRead);
            status.setLastReadDate(LocalDateTime.now());

            if (newPageRead == book.getPageCnt()) {
                status.setReadingStatus("Finished");
                log.info("Book {} marked as Finished for user {}", bookId, userId);
            }

            BookReadingStatus updatedStatus = bookReadingStatusRepository.save(status);
            log.info("Reading progress updated for user {} and book {}. Total pages read: {}",
                    userId, bookId, newPageRead);

            updateGoalsProgress(userId, dto.getPageRead(), bookId);

            BookReadingStatusResponseDTO response = convertToBookReadingStatusResponseDTO(updatedStatus);
            return new ChangeDTO<>(State.OK, "Reading progress updated successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating reading progress: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating reading progress: ", e);
            return new ChangeDTO<>(State.Fail, "Error updating reading progress: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> createReadingGoal(Integer userId, ReadingGoalRequestDTO dto) {
        try {
            log.info("Creating reading goal for user {}", userId);

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                return new ChangeDTO<>(State.Fail_NotFound, "User not found", null);
            }

            List<String> validPeriods = Arrays.asList("1d", "3d", "week", "month", "quarter", "year");
            if (!validPeriods.contains(dto.getPeriod())) {
                log.warn("Invalid period: {}", dto.getPeriod());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid period. Must be one of: 1d, 3d, week, month, quarter, year", null);
            }

            if (!"books_read".equals(dto.getGoalType()) && !"pages_read".equals(dto.getGoalType())) {
                log.warn("Invalid goal type: {}", dto.getGoalType());
                return new ChangeDTO<>(State.Fail_BadData,
                        "Invalid goal type. Must be: books_read or pages_read", null);
            }

            ReadingGoal goal = ReadingGoal.builder()
                    .userId(userId)
                    .period(dto.getPeriod())
                    .startDate(LocalDateTime.now())
                    .amount(dto.getAmount())
                    .goalType(dto.getGoalType())
                    .currentProgress(0)
                    .build();

            ReadingGoal savedGoal = readingGoalRepository.save(goal);
            log.info("Reading goal created with ID: {}", savedGoal.getPgoalId());

            ReadingGoalResponseDTO response = convertToReadingGoalResponseDTO(savedGoal);
            return new ChangeDTO<>(State.OK, "Reading goal created successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when creating reading goal: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error creating reading goal: ", e);
            return new ChangeDTO<>(State.Fail, "Error creating reading goal: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> updateReadingGoal(Integer userId, Integer goalId, UpdateReadingGoalRequestDTO dto) {
        try {
            log.info("Updating reading goal {} for user {}", goalId, userId);

            Optional<ReadingGoal> goalOpt = readingGoalRepository.findByPgoalIdAndUserId(goalId, userId);

            if (goalOpt.isEmpty()) {
                log.warn("Reading goal not found with ID: {} for user {}", goalId, userId);
                return new ChangeDTO<>(State.Fail_NotFound, "Reading goal not found", null);
            }

            ReadingGoal goal = goalOpt.get();

            if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
                List<String> validPeriods = Arrays.asList("1d", "3d", "week", "month", "quarter", "year");
                if (!validPeriods.contains(dto.getPeriod())) {
                    log.warn("Invalid period: {}", dto.getPeriod());
                    return new ChangeDTO<>(State.Fail_BadData,
                            "Invalid period. Must be one of: 1d, 3d, week, month, quarter, year", null);
                }
                goal.setPeriod(dto.getPeriod());
            }

            if (dto.getAmount() != null && dto.getAmount() > 0) {
                goal.setAmount(dto.getAmount());
            }

            if (dto.getGoalType() != null && !dto.getGoalType().isBlank()) {
                if (!"books_read".equals(dto.getGoalType()) && !"pages_read".equals(dto.getGoalType())) {
                    log.warn("Invalid goal type: {}", dto.getGoalType());
                    return new ChangeDTO<>(State.Fail_BadData,
                            "Invalid goal type. Must be: books_read or pages_read", null);
                }
                goal.setGoalType(dto.getGoalType());
            }

            ReadingGoal updatedGoal = readingGoalRepository.save(goal);
            log.info("Reading goal updated with ID: {}", goalId);

            ReadingGoalResponseDTO response = convertToReadingGoalResponseDTO(updatedGoal);
            return new ChangeDTO<>(State.OK, "Reading goal updated successfully", response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when updating reading goal: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error updating reading goal: ", e);
            return new ChangeDTO<>(State.Fail, "Error updating reading goal: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getReadingGoals(Integer userId) {
        try {
            log.debug("Getting reading goals for user {}", userId);

            List<ReadingGoal> goals = readingGoalRepository.findByUserId(userId);

            if (goals.isEmpty()) {
                log.debug("No reading goals found for user {}", userId);

                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("userId", userId);
                emptyResponse.put("totalGoals", 0);
                emptyResponse.put("goals", new ArrayList<>());

                return new ChangeDTO<>(State.OK, "No reading goals found", emptyResponse);
            }

            List<ReadingGoalResponseDTO> goalDTOs = goals.stream()
                    .map(this::convertToReadingGoalResponseDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("totalGoals", goalDTOs.size());
            response.put("goals", goalDTOs);

            log.debug("Retrieved {} reading goals for user {}", goalDTOs.size(), userId);
            return new ChangeDTO<>(State.OK, "Reading goals retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error getting reading goals: ", e);
            return new ChangeDTO<>(State.Fail, "Error getting reading goals: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ChangeDTO<Object> deleteReadingGoal(Integer userId, Integer goalId) {
        try {
            log.info("Deleting reading goal {} for user {}", goalId, userId);

            Optional<ReadingGoal> goalOpt = readingGoalRepository.findByPgoalIdAndUserId(goalId, userId);

            if (goalOpt.isEmpty()) {
                log.warn("Reading goal not found with ID: {} for user {}", goalId, userId);
                return new ChangeDTO<>(State.Fail_NotFound, "Reading goal not found", null);
            }

            readingGoalRepository.delete(goalOpt.get());
            log.info("Reading goal deleted with ID: {}", goalId);

            return new ChangeDTO<>(State.OK, "Reading goal deleted successfully", null);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when deleting reading goal: ", e);
            return handleDataIntegrityViolation(e);
        } catch (Exception e) {
            log.error("Error deleting reading goal: ", e);
            return new ChangeDTO<>(State.Fail, "Error deleting reading goal: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getGoalStats(Integer userId) {
        try {
            log.debug("Getting goal stats for user {}", userId);

            List<ReadingGoal> goals = readingGoalRepository.findByUserId(userId);

            int totalGoals = goals.size();
            int completedGoals = 0;
            int inProgressGoals = 0;
            int failedGoals = 0;

            LocalDateTime currentDate = LocalDateTime.now();

            for (ReadingGoal goal : goals) {
                LocalDateTime endDate = calculateEndDate(goal.getStartDate(), goal.getPeriod());
                Integer currentProgress = calculateGoalProgress(goal);

                if (currentProgress >= goal.getAmount()) {
                    completedGoals++;
                } else if (currentDate.isBefore(endDate)) {
                    inProgressGoals++;
                } else {
                    failedGoals++;
                }
            }

            GoalStatsDTO stats = new GoalStatsDTO(totalGoals, completedGoals, inProgressGoals, failedGoals);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("stats", stats);

            log.debug("Goal stats for user {}: total={}, completed={}, inProgress={}, failed={}",
                    userId, totalGoals, completedGoals, inProgressGoals, failedGoals);
            return new ChangeDTO<>(State.OK, "Goal statistics retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error getting goal stats: ", e);
            return new ChangeDTO<>(State.Fail, "Error getting goal statistics: " + e.getMessage(), null);
        }
    }

    @Transactional(readOnly = true)
    public ChangeDTO<Object> getAccountStats(Integer userId) {
        try {
            log.debug("Getting account stats for user {}", userId);

            Integer totalPagesRead = bookReadingStatusRepository.sumPagesReadByUserId(userId);

            Long totalBooksRead = bookReadingStatusRepository.countFinishedBooksByUserId(userId);

            Long currentlyReadingBooks = bookReadingStatusRepository
                    .countByUserIdAndReadingStatus(userId, "Reading");

            Long planningToReadBooks = bookReadingStatusRepository
                    .countByUserIdAndReadingStatus(userId, "Planning");

            Long delayedBooks = bookReadingStatusRepository
                    .countByUserIdAndReadingStatus(userId, "Delayed");

            Long gaveUpBooks = bookReadingStatusRepository
                    .countByUserIdAndReadingStatus(userId, "GaveUp");

            AccountStatsDTO stats = new AccountStatsDTO(
                    totalBooksRead.intValue(),
                    totalPagesRead,
                    currentlyReadingBooks.intValue(),
                    planningToReadBooks.intValue(),
                    delayedBooks.intValue(),
                    gaveUpBooks.intValue()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("stats", stats);

            log.debug("Account stats for user {}: booksRead={}, pagesRead={}",
                    userId, totalBooksRead, totalPagesRead);
            return new ChangeDTO<>(State.OK, "Account statistics retrieved successfully", response);

        } catch (Exception e) {
            log.error("Error getting account stats: ", e);
            return new ChangeDTO<>(State.Fail, "Error getting account statistics: " + e.getMessage(), null);
        }
    }

    private BookReadingStatusResponseDTO convertToBookReadingStatusResponseDTO(BookReadingStatus status) {
        BookReadingStatusResponseDTO dto = new BookReadingStatusResponseDTO();
        dto.setBrsId(status.getBrsId());
        dto.setUserId(status.getUserId());
        dto.setBookId(status.getBook().getBookId());
        dto.setReadingStatus(status.getReadingStatus());
        dto.setPageRead(status.getPageRead());
        dto.setLastReadDate(status.getLastReadDate());
        dto.setBookPageCnt(status.getBook().getPageCnt());
        dto.setBookTitle(status.getBook().getTitle());
        return dto;
    }

    private ReadingGoalResponseDTO convertToReadingGoalResponseDTO(ReadingGoal goal) {
        ReadingGoalResponseDTO dto = new ReadingGoalResponseDTO();
        dto.setPgoalId(goal.getPgoalId());
        dto.setUserId(goal.getUserId());
        dto.setPeriod(goal.getPeriod());
        dto.setStartDate(goal.getStartDate());
        dto.setAmount(goal.getAmount());
        dto.setGoalType(goal.getGoalType());

        dto.setCurrentProgress(goal.getCurrentProgress());

        dto.setIsCompleted(goal.getCurrentProgress() >= goal.getAmount());

        dto.setEndDate(calculateEndDate(goal.getStartDate(), goal.getPeriod()));

        return dto;
    }

    private Integer calculateGoalProgress(ReadingGoal goal) {
        LocalDateTime endDate = calculateEndDate(goal.getStartDate(), goal.getPeriod());
        LocalDateTime currentDate = LocalDateTime.now();

        if (currentDate.isBefore(goal.getStartDate())) {
            return 0;
        }

        LocalDateTime startDate = goal.getStartDate();
        LocalDateTime aggregationEndDate = currentDate.isBefore(endDate) ? currentDate : endDate;

        if ("pages_read".equals(goal.getGoalType())) {
            List<BookReadingStatus> statuses = bookReadingStatusRepository.findByUserId(goal.getUserId());
            return statuses.stream()
                    .filter(status -> status.getLastReadDate() != null)
                    .filter(status -> !status.getLastReadDate().isBefore(startDate) &&
                            !status.getLastReadDate().isAfter(aggregationEndDate))
                    .mapToInt(BookReadingStatus::getPageRead)
                    .sum();
        } else if ("books_read".equals(goal.getGoalType())) {
            List<BookReadingStatus> statuses = bookReadingStatusRepository.findByUserId(goal.getUserId());
            return (int) statuses.stream()
                    .filter(status -> "Finished".equals(status.getReadingStatus()))
                    .filter(status -> status.getLastReadDate() != null)
                    .filter(status -> !status.getLastReadDate().isBefore(startDate) &&
                            !status.getLastReadDate().isAfter(aggregationEndDate))
                    .count();
        }

        return 0;
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, String period) {
        switch (period) {
            case "1d":
                return startDate.plusDays(1);
            case "3d":
                return startDate.plusDays(3);
            case "week":
                return startDate.plusWeeks(1);
            case "month":
                return startDate.plusMonths(1);
            case "quarter":
                return startDate.plusMonths(3);
            case "year":
                return startDate.plusYears(1);
            default:
                return startDate.plusDays(1);
        }
    }

    private void updateGoalsProgress(Integer userId, Integer pagesRead, Integer bookId) {
        try {
            LocalDateTime currentDate = LocalDateTime.now();

            List<ReadingGoal> activeGoals = readingGoalRepository.findActiveGoalsByUserId(userId, currentDate);

            for (ReadingGoal goal : activeGoals) {
                if ("pages_read".equals(goal.getGoalType())) {
                    goal.setCurrentProgress(goal.getCurrentProgress() + pagesRead);
                    readingGoalRepository.save(goal);

                    log.debug("Updated pages_read goal {} for user {}: +{} pages, total progress: {}",
                            goal.getPgoalId(), userId, pagesRead, goal.getCurrentProgress());
                } else if ("books_read".equals(goal.getGoalType())) {
                    Optional<BookReadingStatus> statusOpt = bookReadingStatusRepository
                            .findByUserIdAndBook_BookId(userId, bookId);

                    if (statusOpt.isPresent() && "Finished".equals(statusOpt.get().getReadingStatus())) {
                        LocalDateTime finishDate = statusOpt.get().getLastReadDate();
                        LocalDateTime goalStartDate = goal.getStartDate();

                        if (finishDate != null && !finishDate.isBefore(goalStartDate)) {
                            goal.setCurrentProgress(goal.getCurrentProgress() + 1);
                            readingGoalRepository.save(goal);

                            log.debug("Book {} marked as finished, updating books_read goal {} for user {}",
                                    bookId, goal.getPgoalId(), userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error updating goals progress: ", e);
        }
    }

    private ChangeDTO<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message.contains("user_id")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "User with specified ID does not exist", null);
        } else if (message.contains("book_id")) {
            return new ChangeDTO<>(State.Fail_BadData,
                    "Book with specified ID does not exist", null);
        } else if (message.contains("unique constraint") && message.contains("book_reading_status_user_id_book_id_key")) {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Reading status already exists for this book and user", null);
        } else {
            return new ChangeDTO<>(State.Fail_Conflict,
                    "Data integrity violation: " + message, null);
        }
    }
}