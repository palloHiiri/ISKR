-- liquibase formatted sql
-- changeset fuzis:1
CREATE FUNCTION BOOKS.CAN_VIEW_COLLECTION(
    p_user_id INTEGER,
    p_collection_id INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    v_collection_confidentiality BOOKS.CONFIDENTIALITY;
    v_has_disallowed BOOLEAN;
    v_has_allowed BOOLEAN;
    v_owner INTEGER;
BEGIN
    SELECT confidentiality, owner_id
    INTO v_collection_confidentiality, v_owner
    FROM BOOKS.BOOK_COLLECTIONS
    WHERE bcols_id = p_collection_id;


    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;


    IF p_user_id = v_owner THEN
        RETURN TRUE;
    END IF;


    SELECT
        EXISTS(SELECT 1 FROM BOOKS.COLLECTION_VIEW_PRIVILEGES
               WHERE bcols_id = p_collection_id
                 AND user_id = p_user_id
                 AND status = 'Disallowed'),
        EXISTS(SELECT 1 FROM BOOKS.COLLECTION_VIEW_PRIVILEGES
               WHERE bcols_id = p_collection_id
                 AND user_id = p_user_id
                 AND status = 'Allowed')
    INTO v_has_disallowed, v_has_allowed;


    IF v_collection_confidentiality = 'Public' THEN
        RETURN NOT v_has_disallowed;
    ELSIF v_collection_confidentiality = 'Private' THEN
        RETURN v_has_allowed;
    END IF;


    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION BOOKS.GET_VISIBLE_COLLECTIONS_FOR_USER(
    p_user_id INTEGER
) RETURNS TABLE(
    bcols_id INTEGER,
    owner_id INTEGER,
    title VARCHAR(512),
    description TEXT,
    confidentiality BOOKS.CONFIDENTIALITY,
    book_collection_type BOOKS.COLLECTION_TYPE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        bc.bcols_id,
        bc.owner_id,
        bc.title,
        bc.description,
        bc.confidentiality,
        bc.book_collection_type
    FROM BOOKS.BOOK_COLLECTIONS bc
    WHERE bc.owner_id = p_user_id
      AND BOOKS.CAN_VIEW_COLLECTION(p_user_id, bc.bcols_id) = TRUE
   
    UNION
   
    SELECT
        bc.bcols_id,
        bc.owner_id,
        bc.title,
        bc.description,
        bc.confidentiality,
        bc.book_collection_type
    FROM BOOKS.LIKED_COLLECTIONS lc
    JOIN BOOKS.BOOK_COLLECTIONS bc ON lc.bcols_id = bc.bcols_id
    WHERE lc.user_id = p_user_id
      AND BOOKS.CAN_VIEW_COLLECTION(p_user_id, bc.bcols_id) = TRUE;
   
END;
$$ LANGUAGE plpgsql;
