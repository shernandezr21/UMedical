package com.umedical.umedical.exception;

/** El usuario esta autenticado pero no puede hacer esto (HTTP 403). */

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}