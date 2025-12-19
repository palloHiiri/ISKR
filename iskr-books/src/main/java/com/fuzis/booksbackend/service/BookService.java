package com.fuzis.booksbackend.service;

import com.fuzis.booksbackend.entity.Book;
import com.fuzis.booksbackend.repository.BookRepository;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.SelectDTO;
import com.fuzis.booksbackend.transfer.state.State;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public ChangeDTO<Book> getBook(Integer id) {
        Optional<Book> res =  bookRepository.findById(id);
        if(res.isPresent()) {
            return new ChangeDTO<>(State.OK, "Book found", res.get());
        }
        return new ChangeDTO<>(State.Fail_NotFound, "Book not found", res.get());
    }

    public ChangeDTO<Object> createBook(String title, String subtitle, String isbn, Integer pageCount, @NotBlank String description) {
        Book new_book = Book.builder()
                .title(title)
                .subtitle(subtitle)
                .isbn(isbn)
                .description(description)
                .pageCnt(pageCount)
            .build();
        bookRepository.save(new_book);
        return new ChangeDTO<>(State.OK, "Book created", new_book);
    }
}
