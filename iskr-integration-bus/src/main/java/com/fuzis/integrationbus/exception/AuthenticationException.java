package com.fuzis.integrationbus.exception;

public class AuthenticationException extends Exception{
    public AuthenticationException(String msg) {
        super(msg);
    }
    public AuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
