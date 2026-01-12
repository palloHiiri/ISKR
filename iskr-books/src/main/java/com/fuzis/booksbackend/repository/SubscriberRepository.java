package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Subscriber;
import com.fuzis.booksbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Integer> {

    Optional<Subscriber> findBySubsUserAndSubsUserOn(User subsUser, User subsUserOn);

    boolean existsBySubsUserAndSubsUserOn(User subsUser, User subsUserOn);

    @Modifying
    @Query("DELETE FROM Subscriber s WHERE s.subsUser = :subsUser AND s.subsUserOn = :subsUserOn")
    void deleteBySubsUserAndSubsUserOn(@Param("subsUser") User subsUser,
                                       @Param("subsUserOn") User subsUserOn);

    Page<Subscriber> findBySubsUser_UserId(@Param("userId") Integer userId, Pageable pageable);

    Page<Subscriber> findBySubsUserOn_UserId(@Param("userId") Integer userId, Pageable pageable);

    long countBySubsUser_UserId(@Param("userId") Integer userId);

    long countBySubsUserOn_UserId(@Param("userId") Integer userId);

    @Query("SELECT s.subsUserOn.userId, COUNT(s) as subscriberCount " +
            "FROM Subscriber s " +
            "GROUP BY s.subsUserOn.userId " +
            "ORDER BY subscriberCount DESC")
    List<Object[]> findPopularUsers();

    @Query("SELECT s.subsUserOn.userId, COUNT(s) " +
            "FROM Subscriber s " +
            "WHERE s.subsUserOn.userId IN :userIds " +
            "GROUP BY s.subsUserOn.userId")
    List<Object[]> findSubscribersCountByUserIds(@Param("userIds") List<Integer> userIds);
}