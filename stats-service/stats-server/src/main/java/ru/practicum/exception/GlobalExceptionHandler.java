package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(final Exception e) {
        log.info("500 {}", e.getMessage(), e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return ResponseEntity.internalServerError().body(
                new ApiError(stackTrace, e.getMessage(), "Непредвиденная ошибка сервера", "500"));
    }

    @ExceptionHandler(ValidationDataException.class)
    public ResponseEntity<ApiError> handleValidationDataException(final ValidationDataException e) {
        log.info("400 {}", e.getMessage(), e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return ResponseEntity.badRequest().body(
                new ApiError(stackTrace, e.getMessage(), "Ошибка валидации данных", "400"));
    }
}