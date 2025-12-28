package com.fuzis.booksbackend.controller;

import com.fuzis.booksbackend.service.BookService;
import com.fuzis.booksbackend.transfer.BookCreateDTO;
import com.fuzis.booksbackend.transfer.BookUpdateDTO;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.util.HttpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final HttpUtil httpUtil;

    @PostMapping
    public ResponseEntity<ChangeDTO<Object>> createBook(
            @Valid @RequestBody BookCreateDTO bookCreateDTO) {
        return httpUtil.handleServiceResponse(bookService.createBook(bookCreateDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeDTO<Object>> getBookById(
            @PathVariable @Min(1) Integer id) {
        return httpUtil.handleServiceResponse(bookService.getBookById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChangeDTO<Object>> updateBook(
            @PathVariable @Min(1) Integer id,
            @Valid @RequestBody BookUpdateDTO bookUpdateDTO) {
        return httpUtil.handleServiceResponse(bookService.updateBook(id, bookUpdateDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ChangeDTO<Object>> deleteBook(
            @PathVariable @Min(1) Integer id) {
        return httpUtil.handleServiceResponse(bookService.deleteBook(id));
    }

    @GetMapping
    public ResponseEntity<ChangeDTO<Object>> getAllBooks(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer batch) {
        return httpUtil.handleServiceResponse(bookService.getAllBooks(page, batch));
    }
}