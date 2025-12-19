package com.fuzis.booksbackend.transfer.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO <T> implements Serializable {
    private String routing_key;
    private String email;
    private EmailType type;
    private T Content;

    public EmailDTO(String email, EmailType type, T Content) {
        this.routing_key = "mail_msg";
        this.email = email;
        this.type = type;
        this.Content = Content;
    }
}
