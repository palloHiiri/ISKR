package com.fuzis.booksbackend.repository.impl;

import com.fuzis.booksbackend.repository.CollectionAccessRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CollectionAccessRepositoryImpl implements CollectionAccessRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Boolean canViewCollection(Integer userId, Integer collectionId) {
        String sql = "SELECT BOOKS.CAN_VIEW_COLLECTION(:userId, :collectionId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("collectionId", collectionId);

        Object result = query.getSingleResult();
        return result != null && (Boolean) result;
    }
}