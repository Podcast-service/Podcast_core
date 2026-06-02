package podcastService.admin.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import podcastService.admin.client.dto.AuthAdminPageResponse;
import podcastService.admin.client.dto.AuthAdminRoleMutationResponse;
import podcastService.admin.client.dto.AuthAdminRoleRequest;
import podcastService.admin.client.dto.AuthAdminUserResponse;
import podcastService.admin.dto.AdminRoleRequest;
import podcastService.admin.dto.AdminUserFilter;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.ConflictException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.common.exception.UnauthorizedException;
import podcastService.common.exception.UpstreamServiceException;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAdminClient {

    private static final String USERS_PATH = "/auth/admin/users";
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "author", "admin");

    private final RestClient authServiceRestClient;

    public AuthAdminPageResponse<AuthAdminUserResponse> getUsers(
            String authorizationHeader,
            AdminUserFilter filter
    ) {
        String bearerHeader = requireBearerHeader(authorizationHeader);
        try {
            AuthAdminPageResponse<AuthAdminUserResponse> response = authServiceRestClient
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(USERS_PATH)
                                .queryParam("page", filter.normalizedPage())
                                .queryParam("size", filter.normalizedSize())
                                .queryParam("sort", filter.normalizedSort().name());
                        if (hasText(filter.q())) {
                            builder.queryParam("q", filter.q().trim());
                        }
                        if (hasText(filter.role())) {
                            builder.queryParam("role", filter.role().trim());
                        }
                        if (filter.emailVerified() != null) {
                            builder.queryParam("emailVerified", filter.emailVerified());
                        }
                        return builder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.items() == null) {
                throw new UpstreamServiceException("Auth-service returned invalid users response");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw mapAuthServiceError(exception, "list users");
        } catch (RestClientException exception) {
            log.warn("Auth-service users request failed, reason={}", exception.getMessage());
            throw new UpstreamServiceException("Auth-service is unavailable", exception);
        }
    }

    public AuthAdminUserResponse getUser(String authorizationHeader, UUID userId) {
        String bearerHeader = requireBearerHeader(authorizationHeader);
        try {
            AuthAdminUserResponse response = authServiceRestClient
                    .get()
                    .uri(USERS_PATH + "/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                    .retrieve()
                    .body(AuthAdminUserResponse.class);

            if (response == null || response.userId() == null) {
                throw new UpstreamServiceException("Auth-service returned invalid user response");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw mapAuthServiceError(exception, "get user");
        } catch (RestClientException exception) {
            log.warn("Auth-service user request failed, userId={}, reason={}", userId, exception.getMessage());
            throw new UpstreamServiceException("Auth-service is unavailable", exception);
        }
    }

    public AuthAdminRoleMutationResponse addRole(
            String authorizationHeader,
            UUID userId,
            AdminRoleRequest request
    ) {
        String roleName = normalizeRole(request.roleName());
        String bearerHeader = requireBearerHeader(authorizationHeader);
        try {
            AuthAdminRoleMutationResponse response = authServiceRestClient
                    .post()
                    .uri(USERS_PATH + "/{userId}/roles", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                    .body(new AuthAdminRoleRequest(roleName))
                    .retrieve()
                    .body(AuthAdminRoleMutationResponse.class);

            return validateRoleMutationResponse(response);
        } catch (RestClientResponseException exception) {
            throw mapAuthServiceError(exception, "add role");
        } catch (RestClientException exception) {
            log.warn("Auth-service add role request failed, userId={}, roleName={}, reason={}",
                    userId, roleName, exception.getMessage());
            throw new UpstreamServiceException("Auth-service is unavailable", exception);
        }
    }

    public AuthAdminRoleMutationResponse removeAdminRole(String authorizationHeader, UUID userId) {
        String bearerHeader = requireBearerHeader(authorizationHeader);
        try {
            AuthAdminRoleMutationResponse response = authServiceRestClient
                    .delete()
                    .uri(USERS_PATH + "/{userId}/roles/admin", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                    .retrieve()
                    .body(AuthAdminRoleMutationResponse.class);

            return validateRoleMutationResponse(response);
        } catch (RestClientResponseException exception) {
            throw mapAuthServiceError(exception, "remove admin role");
        } catch (RestClientException exception) {
            log.warn("Auth-service remove admin role request failed, userId={}, reason={}",
                    userId, exception.getMessage());
            throw new UpstreamServiceException("Auth-service is unavailable", exception);
        }
    }

    private AuthAdminRoleMutationResponse validateRoleMutationResponse(AuthAdminRoleMutationResponse response) {
        if (response == null || response.userId() == null || response.roles() == null) {
            throw new UpstreamServiceException("Auth-service returned invalid role mutation response");
        }
        return response;
    }

    private RuntimeException mapAuthServiceError(RestClientResponseException exception, String operation) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        log.warn(
                "Auth-service admin operation failed, operation={}, status={}, responseLength={}",
                operation,
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString() == null ? 0 : exception.getResponseBodyAsString().length()
        );

        if (status == HttpStatus.UNAUTHORIZED) {
            return new UnauthorizedException("Auth-service rejected current access token");
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new ForbiddenOperationException("Auth-service denied admin operation");
        }
        if (status == HttpStatus.NOT_FOUND) {
            return new NotFoundException("User not found");
        }
        if (status == HttpStatus.CONFLICT) {
            return new ConflictException("Auth-service rejected role mutation");
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Auth-service rejected admin request");
        }
        return new UpstreamServiceException("Auth-service admin operation failed", exception);
    }

    private String normalizeRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toLowerCase();
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new BadRequestException("Invalid role name");
        }
        return normalized;
    }

    private String requireBearerHeader(String authorizationHeader) {
        if (!hasText(authorizationHeader)) {
            throw new UnauthorizedException("Authorization header is required");
        }
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new UnauthorizedException("Authorization header must use Bearer scheme");
        }
        return authorizationHeader.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
