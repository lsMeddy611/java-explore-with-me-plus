package ru.practicum.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(final Exception e) {
        String stackTrace = getStackTrace(HttpStatus.INTERNAL_SERVER_ERROR, e);
        return ResponseEntity.internalServerError().body(
                new ApiError(stackTrace, e.getMessage(), "Непредвиденная ошибка сервера", "500"));
    }

    @ExceptionHandler(ValidationDataException.class)
    public ResponseEntity<ApiError> handleValidationDataException(final ValidationDataException e) {
        String stackTrace = getStackTrace(HttpStatus.BAD_REQUEST, e);
        return ResponseEntity.badRequest().body(
                new ApiError(stackTrace, e.getMessage(), "Ошибка валидации данных", "400"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String stackTrace = getStackTrace(HttpStatus.BAD_REQUEST, e);
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError(stackTrace, errorMessage, "Ошибка валидации данных", "400"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException e) {
        String stackTrace = getStackTrace(HttpStatus.BAD_REQUEST, e);
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError(stackTrace, errorMessage, "Ошибка валидации данных", "400"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        String stackTrace = getStackTrace(HttpStatus.BAD_REQUEST, e);
        String errorMessage = e.getMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError(stackTrace, errorMessage, "Ошибка валидации данных", "400"));
    }

    private String getStackTrace(HttpStatus httpStatus, Exception e) {
        log.info("Status: {}, Message: {}", httpStatus.toString(), e.getMessage(), e);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}