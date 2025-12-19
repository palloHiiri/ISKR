package com.fuzis.booksbackend.entity;

import com.fuzis.booksbackend.entity.enumerate.ReadingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_reading_status", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookReadingStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brs_id")
    private Integer brsId;

    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_status")
    private ReadingStatus readingStatus;

    @Column(name = "page_read")
    private Integer pageRead = 0;

    @Column(name = "last_read_date")
    private LocalDateTime lastReadDate;
}