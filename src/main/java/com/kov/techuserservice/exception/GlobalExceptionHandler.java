package com.kov.techuserservice.exception;

import com.kov.techuserservice.dto.error.ApiError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Единая точка маппинга исключений в {@link ApiError}.
 * Покрывает: 404 (not found), 409 (duplicate), 401/403 (security JSON),
 * 400 (валидация — ВСЕ ошибки, а не только первая), 500 (fallback).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 404 ---

    @ExceptionHandler({UserNotFoundException.class, AddressNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request, null);
    }

    // --- 409 ---

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request, null);
    }

    /**
     * Гонка уникальности на уровне БД (например, два параллельных register
     * с одним email прошли {@code existsByEmail} одновременно).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), e.getMessage());
        return error(HttpStatus.CONFLICT, "Data integrity violation: duplicate or conflicting data", request, null);
    }

    // --- 401 / 403 (JSON, тот же формат что у entry points в SecurityConfig) ---

    @ExceptionHandler({SecurityException.class, BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), request, null);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            org.springframework.security.core.AuthenticationException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), request, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions", request, null);
    }

    // --- 400 (валидация: ВСЕ ошибки) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.merge(fe.getField(), fe.getDefaultMessage(), (a, b) -> a + "; " + b));
        e.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.merge(ge.getObjectName(), ge.getDefaultMessage(), (a, b) -> a + "; " + b));
        String message = errors.isEmpty() ? "Validation failed"
                : "Validation failed: " + errors.entrySet().stream()
                        .map(en -> en.getKey() + ": " + en.getValue())
                        .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "parameter";
            errors.merge(path, v.getMessage(), (a, b) -> a + "; " + b);
        });
        String message = errors.isEmpty() ? "Constraint violation"
                : "Validation failed: " + errors.entrySet().stream()
                        .map(en -> en.getKey() + ": " + en.getValue())
                        .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request, null);
    }

    // --- 500 fallback (без утечки internals) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, null);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message,
                                           HttpServletRequest request, Map<String, String> errors) {
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.name())
                .message(message)
                .path(request != null ? request.getRequestURI() : null)
                .errors(errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
