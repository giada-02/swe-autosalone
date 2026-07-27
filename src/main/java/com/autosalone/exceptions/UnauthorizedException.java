package com.autosalone.exceptions;

public class UnauthorizedException extends SecurityException {
    public UnauthorizedException(String message) {
        super(message);
    }
}