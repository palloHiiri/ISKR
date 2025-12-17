package com.fuzis.mailer.transfer.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO implements Serializable {
    private String routing_key;
    private String email;
    private EmailType type;
    private Object content;

    public EmailDTO(String email, EmailType type, Object Content) {
        this.routing_key = "mail_msg";
        this.email = email;
        this.type = type;
        this.content = Content;
    }
}
