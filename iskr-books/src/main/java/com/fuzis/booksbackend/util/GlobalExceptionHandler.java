package com.fuzis.booksbackend.util;

import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.transfer.state.State;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ChangeDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "Validation failed: " + String.join(", ", errors.values());
        logger.warn("Validation error: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail, errorMessage, null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ChangeDTO<Object>> handleDatabaseExceptions(DataAccessException ex) {
        logger.error("Database error occurred: ", ex);

        if (ex instanceof DataIntegrityViolationException) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ChangeDTO<>(
                            State.Fail,
                            "Database integrity violation: " + ex.getMessage(),
                            null
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChangeDTO<>(
                        State.Fail,
                        "Database operation failed: " + ex.getMessage(),
                        null
                ));
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
        } else {
            errorMessage = "Invalid request body format. Please check your input.";
        }

        logger.warn("Date parsing error: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail, errorMessage, null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ChangeDTO<Object>> handleTypeMismatchExceptions(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format(
                "Invalid value for parameter '%s': '%s'. Expected type: %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        logger.warn("Type mismatch error: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail, errorMessage, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ChangeDTO<Object>> handleConstraintViolationExceptions(ConstraintViolationException ex) {
        String errorMessage = "Validation failed: " + ex.getMessage();

        logger.warn("Constraint violation: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ChangeDTO<>(State.Fail, errorMessage, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChangeDTO<Object>> handleGeneralExceptions(Exception ex) {
        logger.error("Unexpected error occurred: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChangeDTO<>(
                        State.Fail,
                        "An unexpected error occurred. Please contact support.",
                        null
                ));
    }
}