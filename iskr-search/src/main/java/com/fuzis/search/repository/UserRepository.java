package com.fuzis.search.repository;

import com.fuzis.search.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT * FROM accounts.users ORDER BY id", nativeQuery = true)
    List<User> findAllOrderedById();
}
