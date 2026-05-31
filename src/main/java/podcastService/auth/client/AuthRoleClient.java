package podcastService.auth.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import podcastService.auth.dto.AuthRoleUpdateRequest;
import podcastService.auth.dto.AuthRoleUpdateResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.UnauthorizedException;
import podcastService.common.exception.UpstreamServiceException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRoleClient {

    private static final String UPDATE_ROLES_PATH = "/auth/me/update-roles";

    private final RestClient authServiceRestClient;

    public AuthRoleUpdateResponse addRole(String authorizationHeader, String roleName) {
        String bearerHeader = requireBearerHeader(authorizationHeader);
        try {
            AuthRoleUpdateResponse response = authServiceRestClient
                    .post()
                    .uri(UPDATE_ROLES_PATH)
                    .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                    .body(new AuthRoleUpdateRequest(roleName))
                    .retrieve()
                    .body(AuthRoleUpdateResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                log.warn("Auth-service returned empty role update response, roleName={}", roleName);
                throw new UpstreamServiceException("Auth-service returned invalid role update response");
            }
            if (response.expiresIn() <= 0) {
                log.warn("Auth-service returned invalid token expiration, roleName={}, expiresIn={}",
                        roleName, response.expiresIn());
                throw new UpstreamServiceException("Auth-service returned invalid token expiration");
            }

            log.info("Auth-service role update succeeded, roleName={}, expiresIn={}", roleName, response.expiresIn());
            return response;
        } catch (RestClientResponseException exception) {
            throw mapAuthServiceError(exception, roleName);
        } catch (RestClientException exception) {
            log.warn("Auth-service role update request failed, roleName={}, reason={}", roleName, exception.getMessage());
            throw new UpstreamServiceException("Auth-service is unavailable");
        }
    }

    private RuntimeException mapAuthServiceError(RestClientResponseException exception, String roleName) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        log.warn(
                "Auth-service rejected role update, roleName={}, status={}, responseLength={}",
                roleName,
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString() == null ? 0 : exception.getResponseBodyAsString().length()
        );

        if (status == HttpStatus.UNAUTHORIZED) {
            return new UnauthorizedException("Auth-service rejected current access token");
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new ForbiddenOperationException("Auth-service denied role update");
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Auth-service rejected role update request");
        }
        return new UpstreamServiceException("Auth-service failed to update user role");
    }

    private String requireBearerHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Authorization header is required");
        }
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            throw new UnauthorizedException("Authorization header must use Bearer scheme");
        }
        return authorizationHeader.trim();
    }
}
