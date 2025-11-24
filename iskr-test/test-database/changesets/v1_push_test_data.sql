-- liquibase formatted sql
-- changeset fuzis:1
INSERT INTO ACCOUNTS.USERS (user_id, username, registered_date) VALUES 
(1, 'alex123', '2023-01-15 10:30:00'),
(2, 'pallo_hiiri', '2023-02-20 14:45:00'),
(3, 'testuser', '2023-03-10 09:15:00'),
(4, 'anonymous', '2023-04-05 16:20:00'),
(5, 'booklover', '2023-05-12 11:00:00');

INSERT INTO ACCOUNTS.TOKEN_TYPES (tt_id, tt_name) VALUES 
(1, 'general_acceptance_token'),
(2, 'verify_email_token'),
(3, 'reset_password_token'),

-- INSERT INTO ACCOUNTS.TOKENS (ct_id, token_key, till_date, token_type, token_body) VALUES 
-- (1, '79asd678f6578df86d86af8da', '2024-01-15 10:30:00', 1, ''),
-- (2, '7s9d78a9798r983h9r3hc988c', '2024-01-15 10:30:00', 2, ''),
-- (3, 'f88ds90f89y79dfdt798f9fd9', '2023-06-15 10:30:00', 3, ''),
-- (4, 's79d79as7987d98as987d980a', '2023-07-20 14:45:00', 4, ''),
-- (5, 'a768s5d887s6as9s7sa97as99', '2025-05-12 11:00:00', 5, ''),
-- (6, '76sad67a78asdd6as87d9d6s8', '2023-06-15 10:30:00', 3, ''),
-- (7, 'huouuou23oo32h323huh2ohou', '2023-06-15 10:30:00', 3, ''),
-- (8, '323uoyhuoi2h3oiuoho2i2ih3', '2023-06-15 10:30:00', 3, ''),
-- (9, '5oi3h4huhuiigiuiuuohhoohh', '2023-06-15 10:30:00', 3, '');

-- INSERT INTO ACCOUNTS.RESET_PASSWORD_REQUESTS (rpr_id, user_id, request_date, token_id) VALUES 
-- (1, 2, '2023-05-20 10:30:00', 3),
-- (2, 3, '2023-06-01 14:15:00', 6),
-- (3, 1, '2023-04-10 09:45:00', 7),
-- (4, 4, '2023-07-05 16:30:00', 8),
-- (5, 5, '2023-08-12 11:20:00', 9);

INSERT INTO IMAGES.IMAGE_DATAS (imgd_id, uuid, uploader_id, size) VALUES 
(1, '11111111-1111-1111-1111-111111111111', 1, 1024),
(2, '22222222-2222-2222-2222-222222222222', 2, 2048),
(3, '33333333-3333-3333-3333-333333333333', 3, 3072),
(4, '44444444-4444-4444-4444-444444444444', 4, 4096),
(5, '55555555-5555-5555-5555-555555555555', 5, 5120);

INSERT INTO IMAGES.IMAGE_LINKS (imgl_id, imgd_id) VALUES 
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5);

INSERT INTO ACCOUNTS.USER_PROFILES (up_id, user_id, user_imgl_id, nickname, email, profile_description, birth_date, status) VALUES 
(1, 1, 1, 'Alex', 'alex@example.com', 'Book enthusiast', '1990-01-15', 'notBanned'),
(2, 2, 2, 'SomePerson', 'person@example.com', 'Who am I?', '1985-05-20', 'notBanned'),
(3, 3, 3, 'TestUser', 'test@example.com', 'Just testing', '1995-08-10', 'notBanned'),
(4, 4, 4, 'Anon', 'anon@example.com', 'Anonymous reader', '1992-03-25', 'banned'),
(5, 5, 5, 'BookLover', 'booklover@example.com', 'Love books', '1988-12-05', 'notBanned');

INSERT INTO BOOKS.GENRES (genre_id, name) VALUES 
(1, 'Science Fiction'),
(2, 'Fantasy'),
(3, 'Mystery'),
(4, 'Romance'),
(5, 'Biography');

INSERT INTO BOOKS.AUTHORS (author_id, name, birth_date, description, real_name) VALUES 
(1, 'Isaac Asimov', '1920-01-02', 'Science fiction writer', 'Isaac Asimov'),
(2, 'J.K. Rowling', '1965-07-31', 'Harry Potter author', 'Joanne Rowling'),
(3, 'Agatha Christie', '1890-09-15', 'Mystery novelist', 'Agatha Christie'),
(4, 'Jane Austen', '1775-12-16', 'Classic literature', 'Jane Austen'),
(5, 'Walter Isaacson', '1952-05-20', 'Biographer', 'Walter Isaacson');

INSERT INTO BOOKS.BOOKS (book_id, isbn, title, subtitle, description, page_cnt, photo_link, added_by) VALUES 
(1, '978-0553293357', 'Foundation', 'The First Foundation Novel', 'Epic science fiction series', 255, 1, 1),
(2, '978-0439708180', 'Harry Potter and the Sorcerer''s Stone', NULL, 'First Harry Potter book', 320, 2, 2),
(3, '978-0062073501', 'Murder on the Orient Express', NULL, 'Classic mystery novel', 274, 3, 3),
(4, '978-0141439518', 'Pride and Prejudice', NULL, 'Romantic classic', 432, 4, 4),
(5, '978-1501127625', 'Steve Jobs', 'The Exclusive Biography', 'Biography of Apple founder', 656, 5, 5);

INSERT INTO BOOKS.BOOKS_AUTHORS (gc_book_author_id, book_id, author_id) VALUES 
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5);

INSERT INTO BOOKS.BOOKS_GENRES (gc_book_genre_id, book_id, genre_id) VALUES 
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5);

INSERT INTO BOOKS.BOOK_REVIEWS (rvw_id, user_id, book_id, score, review_text) VALUES 
(1, 1, 2, 5, 'Great start to an amazing series!'),
(2, 2, 1, 4, 'Classic sci-fi at its best'),
(3, 3, 3, 5, 'The perfect mystery novel'),
(4, 4, 4, 4, 'Beautiful romance story'),
(5, 5, 5, 5, 'Fascinating biography');

INSERT INTO BOOKS.READING_GOALS (pgoal_id, user_id, period, start_date, amount, goal_type) VALUES 
(1, 1, 'month', '2023-09-01', 5, 'books_read'),
(2, 2, 'year', '2023-01-01', 50, 'books_read'),
(3, 3, 'week', '2023-09-10', 1, 'books_read'),
(4, 4, 'month', '2023-09-01', 1000, 'pages_read'),
(5, 5, 'quarter', '2023-07-01', 15, 'books_read');

INSERT INTO BOOKS.BOOK_READING_STATUS (brs_id, user_id, book_id, reading_status, page_read, last_read_date) VALUES 
(1, 1, 1, 'Reading', 100, '2023-09-15 10:00:00'),
(2, 2, 2, 'Reading', 200, '2023-09-14 15:30:00'),
(3, 3, 3, 'Planning', 0, NULL),
(4, 4, 4, 'Finished', 432, '2023-09-10 12:00:00'),
(5, 5, 5, 'Reading', 600, '2023-09-13 14:20:00');

INSERT INTO BOOKS.BOOK_COLLECTIONS (bcols_id, owner_id, title, description, confidentiality, book_collection_type) VALUES 
(1, 1, 'My Sci-Fi Favorites', 'Best science fiction books', 'Public', 'Standard'),
(2, 2, 'Childhood Favorites', 'Books from my childhood', 'Private', 'Liked'),
(3, 3, 'To Read Next', 'My reading list', 'Public', 'Wishlist'),
(4, 4, 'Classic Literature', 'Timeless classics', 'Public', 'Standard'),
(5, 5, 'Biography Collection', 'Inspirational life stories', 'Private', 'Standard');

INSERT INTO BOOKS.BOOKS_BOOK_COLLECTIONS (c_book_bcol_id, book_id, bcols_id) VALUES 
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5);

INSERT INTO BOOKS.COLLECTION_VIEW_PRIVILEGES (cvp_id, bcols_id, user_id, status) VALUES 
(1, 1, 2, 'Allowed'),
(2, 1, 3, 'Allowed'),
(3, 4, 1, 'Allowed'),
(4, 4, 5, 'Disallowed'),
(5, 3, 2, 'Allowed');

INSERT INTO BOOKS.LIKED_COLLECTIONS (lc_id, user_id, bcols_id) VALUES 
(1, 2, 1),
(2, 3, 1),
(3, 1, 4),
(4, 5, 3),
(5, 4, 2);

INSERT INTO BOOKS.SUBSCRIBERS (subs_id, subs_user_id, subs_user_on_id) VALUES 
(1, 2, 1),
(2, 3, 1),
(3, 1, 2),
(4, 4, 3),
(5, 5, 4);