package podcastService.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import podcastService.common.exception.ApiErrorResponse;
import podcastService.common.exception.ErrorCode;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), message);
    }

    public void writeForbidden(HttpServletResponse response, String message) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), message);
    }

    private void write(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
