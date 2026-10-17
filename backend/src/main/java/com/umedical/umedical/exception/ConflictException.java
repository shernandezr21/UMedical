package com.umedical.umedical.exception;

/** La operacion choca con el estado actual de los datos (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}