package ru.practicum.exception;

import java.time.LocalDateTime;

public record ApiError(
        String error,
        String message,
        String reason,
        String status,
        LocalDateTime timestamp
) {
    public ApiError(String error, String message, String reason, String status) {
        this(error, message, reason, status, LocalDateTime.now());
    }
}
