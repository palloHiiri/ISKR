package com.fuzis.accountsbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="token_types")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TokenType {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="tt_id")
    private Integer ttId;

    @Column(name="tt_name")
    private String ttName;

    public TokenType(String ttName) {
        this.ttName = ttName;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "tokenType")
    private List<Token> tokens;
}
