package podcastService.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
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


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        String fieldName = exception.getName();
        Object rejectedValue = exception.getValue();

        if (exception.getRequiredType() != null && exception.getRequiredType().isEnum()) {
            Class<?> enumClass = exception.getRequiredType();
            Object[] enumConstants = enumClass.getEnumConstants();

            String allowedValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            details.put(fieldName, String.format(
                    "Invalid value '%s'. Allowed values: [%s]",
                    rejectedValue, allowedValues));
        } else {
            details.put(fieldName, "Invalid value format");
        }

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
}
