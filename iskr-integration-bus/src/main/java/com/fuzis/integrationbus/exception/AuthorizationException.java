package com.fuzis.integrationbus.exception;

public class AuthorizationException extends Exception{
    public AuthorizationException(String msg) {
        super(msg);
    }
    public AuthorizationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
