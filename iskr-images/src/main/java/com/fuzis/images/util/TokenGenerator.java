package com.fuzis.images.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenGenerator
{
    public String getTokenKey(){
        return UUID.randomUUID().toString();
    }
}
