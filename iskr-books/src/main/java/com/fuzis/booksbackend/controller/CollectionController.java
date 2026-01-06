    package com.fuzis.booksbackend.controller;
    
    import com.fuzis.booksbackend.service.CollectionService;
    import com.fuzis.booksbackend.transfer.*;
    import com.fuzis.booksbackend.util.HttpUtil;
    import jakarta.validation.Valid;
    import jakarta.validation.constraints.Min;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.ResponseEntity;
    import org.springframework.validation.annotation.Validated;
    import org.springframework.web.bind.annotation.*;
    
    @Validated
    @RestController
    @RequestMapping("/api/v1/collections")
    @RequiredArgsConstructor
    public class CollectionController {
    
        private final CollectionService collectionService;
        private final HttpUtil httpUtil;
    
        @GetMapping("/{collectionId}")
        public ResponseEntity<ChangeDTO<Object>> getCollectionDetail(
                @PathVariable @Min(1) Integer collectionId,
                @RequestHeader(required = false) Integer userId) {
            return httpUtil.handleServiceResponse(collectionService.getCollectionDetail(collectionId, userId));
        }
    
        @GetMapping("/{collectionId}/books")
        public ResponseEntity<ChangeDTO<Object>> getCollectionBooks(
                @PathVariable @Min(1) Integer collectionId,
                @RequestHeader(required = false) Integer userId,
                @RequestParam(defaultValue = "0") @Min(0) Integer page,
                @RequestParam(defaultValue = "10") @Min(1) Integer batch) {
            return httpUtil.handleServiceResponse(
                    collectionService.getCollectionBooks(collectionId, userId, page, batch)
            );
        }
    
        @PostMapping
        public ResponseEntity<ChangeDTO<Object>> createCollection(
                @RequestHeader(required = false) Integer userId,
                @Valid @RequestBody CollectionRequestDTO collectionRequestDTO) {
            return httpUtil.handleServiceResponse(
                    collectionService.createCollection(userId, collectionRequestDTO)
            );
        }
    
        @PutMapping("/{collectionId}")
        public ResponseEntity<ChangeDTO<Object>> updateCollection(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @Valid @RequestBody CollectionRequestDTO collectionRequestDTO) {
            return httpUtil.handleServiceResponse(
                    collectionService.updateCollection(userId, collectionId, collectionRequestDTO)
            );
        }
    
        @DeleteMapping("/{collectionId}")
        public ResponseEntity<ChangeDTO<Object>> deleteCollection(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId) {
            return httpUtil.handleServiceResponse(
                    collectionService.deleteCollection(userId, collectionId)
            );
        }
    
        @PostMapping("/{collectionId}/books")
        public ResponseEntity<ChangeDTO<Object>> addBookToCollection(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @Valid @RequestBody AddBookRequestDTO addBookRequestDTO) {
            return httpUtil.handleServiceResponse(
                    collectionService.addBookToCollection(userId, collectionId, addBookRequestDTO.getBookId())
            );
        }
    
        @DeleteMapping("/{collectionId}/books/{bookId}")
        public ResponseEntity<ChangeDTO<Object>> removeBookFromCollection(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @PathVariable @Min(1) Integer bookId) {
            return httpUtil.handleServiceResponse(
                    collectionService.removeBookFromCollection(userId, collectionId, bookId)
            );
        }
    
        @PostMapping("/{collectionId}/privileges")
        public ResponseEntity<ChangeDTO<Object>> addCollectionPrivilege(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @Valid @RequestBody CollectionPrivilegeRequestDTO privilegeRequestDTO) {
            return httpUtil.handleServiceResponse(
                    collectionService.addCollectionPrivilege(userId, collectionId, privilegeRequestDTO)
            );
        }
    
        @DeleteMapping("/{collectionId}/privileges/{privilegeUserId}")
        public ResponseEntity<ChangeDTO<Object>> removeCollectionPrivilege(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @PathVariable @Min(1) Integer privilegeUserId) {
            return httpUtil.handleServiceResponse(
                    collectionService.removeCollectionPrivilege(userId, collectionId, privilegeUserId)
            );
        }
    
        @PostMapping("/{collectionId}/likes")
        public ResponseEntity<ChangeDTO<Object>> likeCollection(
                @RequestHeader Integer userId,
                @PathVariable @Min(1) Integer collectionId) {
            return httpUtil.handleServiceResponse(
                    collectionService.likeCollection(userId, collectionId)
            );
        }
    
        @DeleteMapping("/{collectionId}/likes")
        public ResponseEntity<ChangeDTO<Object>> unlikeCollection(
                @RequestHeader Integer userId,
                @PathVariable @Min(1) Integer collectionId) {
            return httpUtil.handleServiceResponse(
                    collectionService.unlikeCollection(userId, collectionId)
            );
        }
    
        // Новый эндпоинт: проверка, лайкнул ли пользователь коллекцию
        @GetMapping("/{collectionId}/likes/me")
        public ResponseEntity<ChangeDTO<Object>> checkIfLikedCollection(
                @RequestHeader Integer userId,
                @PathVariable @Min(1) Integer collectionId) {
            return httpUtil.handleServiceResponse(
                    collectionService.checkIfLikedCollection(userId, collectionId)
            );
        }
    
        // Новый эндпоинт: получение всех CVP коллекции
        @GetMapping("/{collectionId}/privileges")
        public ResponseEntity<ChangeDTO<Object>> getCollectionPrivileges(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId) {
            return httpUtil.handleServiceResponse(
                    collectionService.getCollectionPrivileges(userId, collectionId)
            );
        }
    
        @GetMapping("/me")
        public ResponseEntity<ChangeDTO<Object>> getMyCollections(
                @RequestHeader(required = false) Integer userId,
                @RequestParam(defaultValue = "0") @Min(0) Integer page,
                @RequestParam(defaultValue = "10") @Min(1) Integer batch) {
            return httpUtil.handleServiceResponse(
                    collectionService.getMyCollections(userId, page, batch)
            );
        }
    
        @GetMapping("/{collectionId}/books/{bookId}/exists")
        public ResponseEntity<ChangeDTO<Object>> checkBookInCollection(
                @RequestHeader(required = false) Integer userId,
                @PathVariable @Min(1) Integer collectionId,
                @PathVariable @Min(1) Integer bookId) {
            return httpUtil.handleServiceResponse(
                    collectionService.checkBookInCollection(userId, collectionId, bookId)
            );
        }
    
        @GetMapping("/wishlist/check")
        public ResponseEntity<ChangeDTO<Object>> checkWishlist(
                @RequestHeader Integer userId) {
            return httpUtil.handleServiceResponse(
                    collectionService.checkUserWishlist(userId)
            );
        }
    
        // Новый эндпоинт: добавление книги в вишлист
        @PostMapping("/wishlist/books")
        public ResponseEntity<ChangeDTO<Object>> addBookToWishlist(
                @RequestHeader Integer userId,
                @Valid @RequestBody AddBookRequestDTO addBookRequestDTO) {
            return httpUtil.handleServiceResponse(
                    collectionService.addBookToWishlist(userId, addBookRequestDTO.getBookId())
            );
        }
    
        // Новый эндпоинт: удаление книги из вишлиста
        @DeleteMapping("/wishlist/books/{bookId}")
        public ResponseEntity<ChangeDTO<Object>> removeBookFromWishlist(
                @RequestHeader Integer userId,
                @PathVariable @Min(1) Integer bookId) {
            return httpUtil.handleServiceResponse(
                    collectionService.removeBookFromWishlist(userId, bookId)
            );
        }
    
        // Новый эндпоинт: полная очистка вишлиста
        @DeleteMapping("/wishlist/books")
        public ResponseEntity<ChangeDTO<Object>> clearWishlist(
                @RequestHeader Integer userId) {
            return httpUtil.handleServiceResponse(
                    collectionService.clearWishlist(userId)
            );
        }
    
        @GetMapping("/wishlist/books/{bookId}/exists")
        public ResponseEntity<ChangeDTO<Object>> checkBookInWishlist(
                @RequestHeader Integer userId,
                @PathVariable @Min(1) Integer bookId) {
            return httpUtil.handleServiceResponse(
                    collectionService.checkBookInWishlist(userId, bookId)
            );
        }
    }