package com.fuzis.search.service;

import com.fuzis.search.entity.Author;
import com.fuzis.search.entity.Book;
import com.fuzis.search.entity.BookCollection;
import com.fuzis.search.entity.Genre;
import com.fuzis.search.entity.User;
import com.fuzis.search.entity.elasticsearch.AuthorDocument;
import com.fuzis.search.entity.elasticsearch.BaseIndexDocument;
import com.fuzis.search.entity.elasticsearch.BookCollectionDocument;
import com.fuzis.search.entity.elasticsearch.BookDocument;
import com.fuzis.search.entity.elasticsearch.GenreDocument;
import com.fuzis.search.entity.elasticsearch.UserDocument;
import com.fuzis.search.repository.*;
import com.fuzis.search.repository.elasticsearch.SearchDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(name = "sync.enabled", havingValue = "true")
@EnableScheduling
public class DataSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCollectionRepository collectionRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookReviewRepository bookReviewRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private LikedCollectionRepository likedCollectionRepository;

    @Autowired
    private BooksBookCollectionsRepository booksBookCollectionsRepository;

    @Autowired
    private SearchDocumentRepository searchDocumentRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${sync.batch-size:1000}")
    private int batchSize;

    private volatile boolean isSyncInProgress = false;

    @PostConstruct
    public void init() {
        createIndexIfNotExists();
    }

    @Scheduled(cron = "${sync.cron}")
    public void syncAllData() {
        if (isSyncInProgress) {
            log.warn("Sync already in progress");
            return;
        }

        try {
            isSyncInProgress = true;
            log.info("Starting Elasticsearch sync");

            long startTime = System.currentTimeMillis();

            syncUsers();
            syncBooks();
            syncCollections();
            syncGenres();
            syncAuthors();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Sync finished, time: {} ms", duration);

        } catch (Exception e) {
            log.error("Sync error", e);
        } finally {
            isSyncInProgress = false;
        }
    }

    private void syncUsers() {
        log.info("Syncing users...");

        try {
            searchDocumentRepository.deleteByType("user");
        } catch (Exception e) {
            log.warn("Could not delete users, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        Page<User> userPage;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            userPage = userRepository.findAll(pageable);
            List<User> users = userPage.getContent();

            if (!users.isEmpty()) {
                List<Integer> userIds = users.stream()
                        .map(User::getUserId)
                        .collect(Collectors.toList());

                List<Object[]> subscribersData = subscriberRepository.findSubscribersCountByUserIds(userIds);
                Map<Integer, Long> subscribersMap = new HashMap<>();
                for (Object[] data : subscribersData) {
                    subscribersMap.put((Integer) data[0], (Long) data[1]);
                }

                for (User user : users) {
                    user.setSubscribersCount(subscribersMap.getOrDefault(user.getUserId(), 0L));
                }

                List<BaseIndexDocument> documents = users.stream()
                        .map(UserDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Synced {} users (page {})", documents.size(), page + 1);
            }

            page++;
        } while (userPage.hasNext());
    }

    private void syncBooks() {
        log.info("Syncing books...");

        try {
            searchDocumentRepository.deleteByType("book");
        } catch (Exception e) {
            log.warn("Could not delete books, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        Page<Integer> bookIdsPage;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            bookIdsPage = bookRepository.findAllBookIds(pageable);
            List<Integer> bookIds = bookIdsPage.getContent();

            if (!bookIds.isEmpty()) {
                List<Book> booksWithImages = bookRepository.findBooksWithImagesByIds(bookIds);
                Map<Integer, Book> booksMap = booksWithImages.stream()
                        .collect(Collectors.toMap(Book::getBookId, book -> book));

                List<Book> booksWithGenres = bookRepository.findBooksWithGenresByIds(bookIds);
                for (Book bookWithGenres : booksWithGenres) {
                    Book book = booksMap.get(bookWithGenres.getBookId());
                    if (book != null) {
                        book.setGenres(bookWithGenres.getGenres());
                    }
                }

                List<Book> booksWithAuthors = bookRepository.findBooksWithAuthorsByIds(bookIds);
                for (Book bookWithAuthors : booksWithAuthors) {
                    Book book = booksMap.get(bookWithAuthors.getBookId());
                    if (book != null) {
                        book.setAuthors(bookWithAuthors.getAuthors());
                    }
                }

                List<Object[]> averageRatings = bookReviewRepository.findAverageRatingsByBookIds(bookIds);
                Map<Integer, Double> ratingsMap = new HashMap<>();
                for (Object[] rating : averageRatings) {
                    ratingsMap.put((Integer) rating[0], (Double) rating[1]);
                }

                List<Object[]> collectionsData = booksBookCollectionsRepository.findCollectionsCountByBookIds(bookIds);
                Map<Integer, Long> collectionsMap = new HashMap<>();
                for (Object[] data : collectionsData) {
                    collectionsMap.put((Integer) data[0], (Long) data[1]);
                }

                List<Book> books = new ArrayList<>(booksMap.values());
                for (Book book : books) {
                    book.setAverageRating(ratingsMap.getOrDefault(book.getBookId(), 0.0));
                    book.setCollectionsCount(collectionsMap.getOrDefault(book.getBookId(), 0L));
                }

                books.sort(Comparator.comparing(Book::getBookId));

                List<BaseIndexDocument> documents = books.stream()
                        .map(BookDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Synced {} books (page {})", documents.size(), page + 1);
            }

            page++;
        } while (bookIdsPage.hasNext());
    }

    private void syncCollections() {
        log.info("Syncing book collections...");

        try {
            searchDocumentRepository.deleteByType("collection");
        } catch (Exception e) {
            log.warn("Could not delete collections, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        Page<Integer> collectionIdsPage;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            collectionIdsPage = collectionRepository.findAllPublicCollectionIds(pageable);
            List<Integer> collectionIds = collectionIdsPage.getContent();

            if (!collectionIds.isEmpty()) {
                List<BookCollection> collections = collectionRepository.findAllById(collectionIds);

                Pageable imagesPageable = Pageable.unpaged();
                Page<BookCollection> collectionsWithImages = collectionRepository.findAllPublicWithImages(imagesPageable);
                Map<Integer, BookCollection> collectionsWithImagesMap = collectionsWithImages.getContent().stream()
                        .collect(Collectors.toMap(BookCollection::getBcolsId, collection -> collection));

                for (BookCollection collection : collections) {
                    BookCollection collectionWithImage = collectionsWithImagesMap.get(collection.getBcolsId());
                    if (collectionWithImage != null) {
                        collection.setPhotoLink(collectionWithImage.getPhotoLink());
                    }
                }

                List<Object[]> likesData = likedCollectionRepository.findLikesCountByCollectionIds(collectionIds);
                Map<Integer, Long> likesMap = new HashMap<>();
                for (Object[] data : likesData) {
                    likesMap.put((Integer) data[0], (Long) data[1]);
                }

                List<Object[]> booksData = booksBookCollectionsRepository.findBookCountByCollectionIds(collectionIds);
                Map<Integer, Integer> booksMap = new HashMap<>();
                for (Object[] data : booksData) {
                    booksMap.put((Integer) data[0], ((Long) data[1]).intValue());
                }

                for (BookCollection collection : collections) {
                    collection.setLikesCount(likesMap.getOrDefault(collection.getBcolsId(), 0L));
                    collection.setBookCount(booksMap.getOrDefault(collection.getBcolsId(), 0));
                }

                collections.sort(Comparator.comparing(BookCollection::getBcolsId));

                List<BaseIndexDocument> documents = collections.stream()
                        .map(BookCollectionDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Synced {} collections (page {})", documents.size(), page + 1);
            }

            page++;
        } while (collectionIdsPage.hasNext());
    }

    private void syncGenres() {
        log.info("Syncing genres...");

        try {
            searchDocumentRepository.deleteByType("genre");
        } catch (Exception e) {
            log.warn("Could not delete genres, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        Page<Genre> genrePage;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            genrePage = genreRepository.findAll(pageable);
            List<Genre> genres = genrePage.getContent();

            if (!genres.isEmpty()) {
                List<BaseIndexDocument> documents = genres.stream()
                        .map(GenreDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Synced {} genres (page {})", documents.size(), page + 1);
            }

            page++;
        } while (genrePage.hasNext());
    }

    private void syncAuthors() {
        log.info("Syncing authors...");

        try {
            searchDocumentRepository.deleteByType("author");
        } catch (Exception e) {
            log.warn("Could not delete authors, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        Page<Author> authorPage;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            authorPage = authorRepository.findAll(pageable);
            List<Author> authors = authorPage.getContent();

            if (!authors.isEmpty()) {
                List<BaseIndexDocument> documents = authors.stream()
                        .map(AuthorDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Synced {} authors (page {})", documents.size(), page + 1);
            }

            page++;
        } while (authorPage.hasNext());
    }

    private void createIndexIfNotExists() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);

            if (!indexOps.exists()) {
                log.info("Creating Elasticsearch index...");
                indexOps.createWithMapping();
                log.info("Index created successfully");
            } else {
                log.info("Elasticsearch index already exists");
            }
        } catch (Exception e) {
            log.error("Index creation error", e);
            try {
                IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of("global_search"));
                if (!indexOps.exists()) {
                    indexOps.create();
                    log.info("Index created without mapping");
                }
            } catch (Exception ex) {
                log.error("Failed to create index even without mapping", ex);
            }
        }
    }
}