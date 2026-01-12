package com.fuzis.booksbackend.util;

import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.state.State;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ChangeDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "Validation failed: " + String.join(", ", errors.values());
        log.warn("Validation error: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ChangeDTO<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: ", ex);

        String message = ex.getMostSpecificCause().getMessage();
        String userMessage;

        if (message.contains("added_by")) {
            userMessage = "User with specified ID does not exist";
        } else if (message.contains("photo_link")) {
            userMessage = "Image with specified photo link does not exist";
        } else if (message.contains("subs_user_id") || message.contains("subs_user_on_id")) {
            userMessage = "User with specified ID does not exist";
        } else if (message.contains("unique constraint") && message.contains("books_title_subtitle_key")) {
            userMessage = "A book with this title and subtitle combination already exists";
        } else if (message.contains("unique constraint") && message.contains("books_isbn_key")) {
            userMessage = "Book with this ISBN already exists";
        } else if (message.contains("unique constraint") && message.contains("books_photo_link_key")) {
            userMessage = "Book with this photo link already exists";
        } else {
            userMessage = "Data integrity violation: " + message;
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ChangeDTO<>(State.Fail_Conflict, userMessage, null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ChangeDTO<Object>> handleMissingParameterException(MissingServletRequestParameterException ex) {
        String errorMessage = String.format(
                "Missing required parameter: '%s' of type %s",
                ex.getParameterName(),
                ex.getParameterType()
        );

        log.warn("Missing parameter: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, null));
    }

    @ExceptionHandler({DateTimeParseException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ChangeDTO<Object>> handleDateTimeParsingExceptions(Exception ex) {
        String errorMessage;

        if (ex instanceof DateTimeParseException dateTimeEx) {
            errorMessage = String.format(
                    "Invalid date/time format: '%s'. Expected format: yyyy-MM-ddTHH:mm:ssZ or yyyy-MM-dd",
                    dateTimeEx.getParsedString()
            );
        } else if (ex.getCause() instanceof DateTimeParseException dateTimeEx) {
            errorMessage = String.format(
                    "Invalid date/time format: '%s'. Expected format: yyyy-MM-ddTHH:mm:ssZ or yyyy-MM-dd",
                    dateTimeEx.getParsedString()
            );
        } else if (ex instanceof HttpMessageNotReadableException) {
            errorMessage = "Invalid request body format. Please check your input. " + ex.getMessage() + "cause: " + ex.getCause().getMessage();
        } else {
            errorMessage = "Invalid request format";
        }

        log.warn("Request parsing error: {}", errorMessage, ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ChangeDTO<Object>> handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format(
                "Invalid value for parameter '%s': '%s'. Expected type: %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("Type mismatch error: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ChangeDTO<Object>> handleConstraintViolationExceptions(ConstraintViolationException ex) {
        String errorMessage = "Validation failed: " + ex.getMessage();

        log.warn("Constraint violation: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ChangeDTO<Object>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        String errorMessage = String.format(
                "Unsupported media type: %s. Supported types: %s",
                ex.getContentType(),
                ex.getSupportedMediaTypes()
        );

        log.warn("Media type not supported: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ChangeDTO<>(State.Fail_BadData, errorMessage, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ChangeDTO<Object>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        String errorMessage = String.format(
                "Method %s is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                ex.getSupportedHttpMethods()
        );

        log.warn("Method not supported: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ChangeDTO<>(State.Fail, errorMessage, null));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ChangeDTO<Object>> handleNotFoundException(NoHandlerFoundException ex) {
        String errorMessage = String.format(
                "Endpoint %s %s not found",
                ex.getHttpMethod(),
                ex.getRequestURL()
        );

        log.warn("Endpoint not found: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ChangeDTO<>(State.Fail_NotFound, errorMessage, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChangeDTO<Object>> handleGenericExceptions(Exception ex) {
        log.error("Unhandled exception occurred", ex);

        // Return detailed error message to client
        String errorMessage = String.format(
                "Internal server error: %s - %s",
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChangeDTO<>(
                        State.Fail,
                        errorMessage,
                        null
                ));
    }
}