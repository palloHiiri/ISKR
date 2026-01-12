CREATE OR REPLACE FUNCTION BOOKS.GET_VISIBLE_BOOKS_FOR_USER(
    p_user_id INTEGER
) RETURNS TABLE(
    book_id INTEGER,
    isbn VARCHAR(17),
    title VARCHAR(1024),
    subtitle VARCHAR(1024),
    description TEXT,
    page_cnt INTEGER,
    photo_link INTEGER,
    added_by INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        b.book_id,
        b.isbn,
        b.title,
        b.subtitle,
        b.description,
        b.page_cnt,
        b.photo_link,
        b.added_by
    FROM BOOKS.BOOKS b
    WHERE 
        b.added_by = p_user_id
        OR
        EXISTS (
            SELECT 1 
            FROM BOOKS.BOOK_READING_STATUS brs
            WHERE brs.user_id = p_user_id 
                AND brs.book_id = b.book_id
        )
    ORDER BY b.title, b.subtitle;
END;
$$ LANGUAGE plpgsql;
