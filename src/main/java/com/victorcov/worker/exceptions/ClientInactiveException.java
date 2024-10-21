package com.victorcov.worker.exceptions;

public class ClientInactiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientInactiveException(String message) {
        super(message);
    }

    public ClientInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}