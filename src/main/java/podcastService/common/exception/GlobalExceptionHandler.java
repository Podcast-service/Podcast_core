package podcastService.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiErrorResponse> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        log.warn("Base exception on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(exception.getErrorCode().name())
                .message(exception.getMessage())
                .timestamp(Instant.now())
                .details(exception.getDetails())
                .build();

        return ResponseEntity
                .status(exception.getErrorCode().httpStatus())
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> details = Map.of("fields", fields);

        log.warn("Validation failed on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                fields);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message("Request validation failed")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String path = violation.getPropertyPath() != null
                    ? violation.getPropertyPath().toString()
                    : "unknown";

            String field = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;

            fields.putIfAbsent(field, violation.getMessage());
        }

        Map<String, Object> details = Map.of("fields", fields);

        log.warn("Constraint violation on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                fields);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message("Request validation failed")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();

        String fieldName = exception.getName();
        Object rejectedValue = exception.getValue();

        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            Object[] enumConstants = exception.getRequiredType().getEnumConstants();

            String allowedValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            fields.put(fieldName, String.format(
                    "Invalid value '%s'. Allowed values: [%s]",
                    rejectedValue, allowedValues
            ));
        } else {
            fields.put(fieldName, "Invalid value format");
        }

        Map<String, Object> details = Map.of("fields", fields);

        log.warn("Type mismatch on [{} {}]: field '{}' with value '{}'",
                request.getMethod(),
                request.getRequestURI(),
                fieldName,
                rejectedValue);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message("Request validation failed")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn("Malformed JSON on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message("Malformed request body")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMostSpecificCause().getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.CONFLICT.name())
                .message("Request conflicts with existing data or database constraints")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.CONFLICT.httpStatus())
                .body(body);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn("Method authorization denied on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.FORBIDDEN.name())
                .message("You don't have permission to access this resource")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.httpStatus())
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument on [{} {}]: {}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message(exception.getMessage())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error on [{} {}]",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ErrorCode.INTERNAL_ERROR.name())
                .message("Unexpected internal error")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(body);
    }
}
