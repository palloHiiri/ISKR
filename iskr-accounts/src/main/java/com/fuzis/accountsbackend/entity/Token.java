package com.fuzis.accountsbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name="tokens")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer ct_id;

    @Column(name = "token_key")
    private String tokenKey;

    private ZonedDateTime till_date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_type")
    private TokenType tokenType;

    @Column(name = "token_body")
    private String tokenBody;

    public Token(String tokenKey, ZonedDateTime till_date, TokenType tokenType, String tokenBody) {
        this.tokenKey = tokenKey;
        this.till_date = till_date;
        this.tokenType = tokenType;
        this.tokenBody = tokenBody;
    }
}
