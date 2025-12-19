package com.fuzis.search.service;

import com.fuzis.search.entity.Book;
import com.fuzis.search.entity.BookCollection;
import com.fuzis.search.entity.User;
import com.fuzis.search.entity.elasticsearch.BaseIndexDocument;
import com.fuzis.search.entity.elasticsearch.BookCollectionDocument;
import com.fuzis.search.entity.elasticsearch.BookDocument;
import com.fuzis.search.entity.elasticsearch.UserDocument;
import com.fuzis.search.repository.BookCollectionRepository;
import com.fuzis.search.repository.BookRepository;
import com.fuzis.search.repository.UserRepository;
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

import java.util.List;
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
            log.info("Sync start Elasticsearch");

            long startTime = System.currentTimeMillis();

            syncUsers();
            syncBooks();
            syncCollections();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Sync finished, time: {} ms", duration);

        } catch (Exception e) {
            log.error("Sync error", e);
        } finally {
            isSyncInProgress = false;
        }
    }

    private void syncUsers() {
        log.info("Sync users...");

        try {
            searchDocumentRepository.deleteByType("user");
        } catch (Exception e) {
            log.warn("Could not delete users, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        List<User> users;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<User> userPage = userRepository.findAll(pageable);
            users = userPage.getContent();

            if (!users.isEmpty()) {
                List<BaseIndexDocument> documents = users.stream()
                        .map(UserDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Sync {} users (page {})", documents.size(), page + 1);
            }

            page++;
        } while (!users.isEmpty());
    }

    private void syncBooks() {
        log.info("Sync books...");

        try {
            searchDocumentRepository.deleteByType("book");
        } catch (Exception e) {
            log.warn("Could not delete books, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        List<Book> books;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<Book> bookPage = bookRepository.findAll(pageable);
            books = bookPage.getContent();

            if (!books.isEmpty()) {
                List<BaseIndexDocument> documents = books.stream()
                        .map(BookDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Sync {} books (page {})", documents.size(), page + 1);
            }

            page++;
        } while (!books.isEmpty());
    }

    private void syncCollections() {
        log.info("Sync book collections...");

        try {
            searchDocumentRepository.deleteByType("collection");
        } catch (Exception e) {
            log.warn("Could not delete collections, maybe index doesn't exist: {}", e.getMessage());
        }

        IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);
        indexOps.refresh();

        int page = 0;
        List<BookCollection> collections;

        do {
            Pageable pageable = PageRequest.of(page, batchSize);
            Page<BookCollection> collectionPage = collectionRepository.findAll(pageable);
            collections = collectionPage.getContent();

            if (!collections.isEmpty()) {
                List<BaseIndexDocument> documents = collections.stream()
                        .map(BookCollectionDocument::fromEntity)
                        .collect(Collectors.toList());

                searchDocumentRepository.saveAll(documents);
                log.info("Sync {} book collections (page {})", documents.size(), page + 1);
            }

            page++;
        } while (!collections.isEmpty());
    }

    private void createIndexIfNotExists() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(BaseIndexDocument.class);

            if (!indexOps.exists()) {
                log.info("Creating elasticsearch index...");
                // Создаем индекс с настройками и маппингом
                indexOps.createWithMapping();
                log.info("Index created successfully");
            } else {
                log.info("Elasticsearch index already exists");
            }
        } catch (Exception e) {
            log.error("Index creation error", e);
            // В случае ошибки пробуем создать без маппинга
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

    public void triggerManualSync() {
        new Thread(() -> syncAllData()).start();
    }
}