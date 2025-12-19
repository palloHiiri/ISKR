package com.fuzis.booksbackend.controller;

import com.fuzis.booksbackend.entity.Book;
import com.fuzis.booksbackend.service.BookService;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.SelectDTO;
import com.fuzis.booksbackend.util.HttpUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/token")
public class BookController {
    private final BookService bookService;

    private final HttpUtil httpUtil;

    @Autowired
    public BookController(BookService bookService, HttpUtil httpUtil){
        this.bookService = bookService;
        this.httpUtil = httpUtil;
    }

    @PostMapping
    public ResponseEntity<ChangeDTO<Object>> createBook(
            @RequestParam(required = false) @NotBlank String isbn,
            @RequestParam @NotBlank String title,
            @RequestParam(required = false) @NotBlank String subtitle,
            @RequestParam(required = false) @NotBlank String description,
            @RequestParam @Min(0) Integer pageCount) {
        return httpUtil.handleServiceResponse(bookService.createBook(title, subtitle, isbn, pageCount, description));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeDTO<Book>> getBook(@PathVariable @Min(0) Integer id) {
        return httpUtil.handleServiceResponse(bookService.getBook(id));
    }

}
