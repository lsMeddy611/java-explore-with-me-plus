package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(final NotFoundException e) {
        log.info("404 {}", e.getMessage(), e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(stackTrace, e.getMessage(), "Данные не найдены", "404"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(final ConflictException e) {
        log.info("409 {}", e.getMessage(), e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError(stackTrace, e.getMessage(), "Конфликт данных", "409"));
    }

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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(final ValidationException e) {
        log.info("400 {}", e.getMessage(), e);
        StringWriter sw = new StringWriter();     //может подумаем, как сократить эту конструкцию?
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return ResponseEntity.internalServerError().body(
                new ApiError(stackTrace, e.getMessage(), "Ошибка валидации данных", "400"));
    }
}
