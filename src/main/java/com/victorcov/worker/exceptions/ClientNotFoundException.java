package com.victorcov.worker.exceptions;

public class ClientNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}