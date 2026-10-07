package com.chubb.claims.common;

import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  public record ApiError(
      Instant timestamp,
      int status,
      String code,
      String message,
      String correlationId,
      Map<String, String> fieldErrors) {}

  private ResponseEntity<ApiError> error(
      HttpStatus status, String code, String message, Map<String, String> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                Instant.now(), status.value(), code, message, MDC.get("correlationId"), fields));
  }

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<ApiError> notFound(NotFoundException e) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), Map.of());
  }

  @ExceptionHandler({
    DomainException.class,
    OptimisticLockingFailureException.class,
    DataIntegrityViolationException.class
  })
  ResponseEntity<ApiError> conflict(Exception e) {
    String message =
        e instanceof DomainException
            ? e.getMessage()
            : "Concurrent change or conflicting operation; retrieve current claim and retry";
    return error(HttpStatus.CONFLICT, "CLAIM_CONFLICT", message, Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new TreeMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    HandlerMethodValidationException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ApiError> malformed(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "Malformed request or invalid parameter",
        Map.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> resource(Exception e) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", Map.of());
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> method(Exception e) {
    return error(
        HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported", Map.of());
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> media(Exception e) {
    return error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_MEDIA_TYPE",
        "Content type not supported",
        Map.of());
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
  ResponseEntity<ApiError> acceptable(Exception e) {
    return error(
        HttpStatus.NOT_ACCEPTABLE,
        "NOT_ACCEPTABLE",
        "Requested response format not supported",
        Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    log.error("Unexpected request failure type={}", e.getClass().getSimpleName());
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", Map.of());
  }
}
