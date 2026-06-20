package ru.practicum.exception;

public class ValidationDataException extends RuntimeException {
    public ValidationDataException(String message) {
        super(message);
    }
}
