package com.autosalone.exceptions;

public class ForbiddenException extends SecurityException {
    public ForbiddenException(String message) {
        super(message);
    }
}