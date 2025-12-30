package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SUBSCRIBERS", schema = "BOOKS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subs_id")
    private Integer subsId;

    @ManyToOne
    @JoinColumn(name = "subs_user_id", nullable = false)
    private User subsUser;

    @ManyToOne
    @JoinColumn(name = "subs_user_on_id", nullable = false)
    private User subsUserOn;
}