package podcastService.author.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.dto.AuthorProfileResponse;
import podcastService.author.dto.BecomeAuthorResponse;
import podcastService.author.dto.CreateAuthorProfileRequest;
import podcastService.auth.client.AuthRoleClient;
import podcastService.auth.dto.AuthRoleUpdateResponse;
import podcastService.common.exception.UnauthorizedException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BecomeAuthorServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String AUTHORIZATION = "Bearer current-token";

    @Mock
    private AuthRoleClient authRoleClient;

    @Mock
    private AuthorProfileService authorProfileService;

    @Test
    void becomeAuthorUpdatesRoleBeforeCreatingLocalProfile() {
        CreateAuthorProfileRequest request = new CreateAuthorProfileRequest("Иван Петров", "Про backend");
        AuthorProfileResponse profile = authorProfile();
        when(authRoleClient.addRole(AUTHORIZATION, "author"))
                .thenReturn(new AuthRoleUpdateResponse("new-access-token", 1800));
        when(authorProfileService.createOrGet(USER_ID, request))
                .thenReturn(new AuthorProfileCreationResult(profile, true));

        BecomeAuthorResult result = service().becomeAuthor(USER_ID, AUTHORIZATION, request);

        InOrder inOrder = inOrder(authRoleClient, authorProfileService);
        inOrder.verify(authorProfileService).validateCreateRequestForBecomeAuthor(USER_ID, request);
        inOrder.verify(authRoleClient).addRole(AUTHORIZATION, "author");
        inOrder.verify(authorProfileService).createOrGet(USER_ID, request);

        assertThat(result.authorProfileCreated()).isTrue();
        BecomeAuthorResponse response = result.response();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.authorProfile()).isSameAs(profile);
    }

    @Test
    void becomeAuthorDoesNotCreateProfileWhenAuthServiceRejectsToken() {
        CreateAuthorProfileRequest request = new CreateAuthorProfileRequest("Иван Петров", null);
        when(authRoleClient.addRole(AUTHORIZATION, "author"))
                .thenThrow(new UnauthorizedException("Auth-service rejected current access token"));

        assertThatThrownBy(() -> service().becomeAuthor(USER_ID, AUTHORIZATION, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Auth-service rejected current access token");

        verify(authorProfileService, never()).createOrGet(USER_ID, request);
    }

    @Test
    void becomeAuthorDoesNotCallAuthServiceWhenLocalRequestIsInvalid() {
        CreateAuthorProfileRequest request = new CreateAuthorProfileRequest(" a ", null);
        org.mockito.Mockito.doThrow(new podcastService.common.exception.BadRequestException("Request validation failed"))
                .when(authorProfileService)
                .validateCreateRequestForBecomeAuthor(USER_ID, request);

        assertThatThrownBy(() -> service().becomeAuthor(USER_ID, AUTHORIZATION, request))
                .isInstanceOf(podcastService.common.exception.BadRequestException.class)
                .hasMessage("Request validation failed");

        verify(authRoleClient, never()).addRole(AUTHORIZATION, "author");
        verify(authorProfileService, never()).createOrGet(USER_ID, request);
    }

    private BecomeAuthorService service() {
        return new BecomeAuthorService(authRoleClient, authorProfileService);
    }

    private AuthorProfileResponse authorProfile() {
        return new AuthorProfileResponse(
                AUTHOR_ID,
                USER_ID,
                "Иван Петров",
                null,
                "Про backend",
                0,
                false,
                OffsetDateTime.parse("2026-05-31T10:00:00Z")
        );
    }
}
