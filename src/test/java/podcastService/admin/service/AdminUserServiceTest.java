package podcastService.admin.service;

import org.junit.jupiter.api.Test;
import podcastService.admin.client.AuthAdminClient;
import podcastService.admin.client.dto.AuthAdminPageResponse;
import podcastService.admin.client.dto.AuthAdminRoleMutationResponse;
import podcastService.admin.client.dto.AuthAdminUserResponse;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminRoleMutationResponse;
import podcastService.admin.dto.AdminRoleRequest;
import podcastService.admin.dto.AdminUserFilter;
import podcastService.admin.dto.AdminUserResponse;
import podcastService.admin.dto.AdminUserSort;
import podcastService.admin.mapper.AdminMapper;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private static final String AUTH_HEADER = "Bearer admin-token";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    private final AuthAdminClient authAdminClient = mock(AuthAdminClient.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final AuthorRepository authorRepository = mock(AuthorRepository.class);
    private final AdminUserService service = new AdminUserService(
            authAdminClient,
            userProfileRepository,
            authorRepository,
            new AdminMapper()
    );

    @Test
    void getUsersEnrichesAuthUsersWithLocalProfiles() {
        AdminUserFilter filter = new AdminUserFilter(null, null, null, 0, 20, AdminUserSort.DATE_DESC);
        AuthAdminUserResponse authUser = authUser();
        UserProfileEntity profile = profile();
        AuthorEntity author = author(profile);
        when(authAdminClient.getUsers(AUTH_HEADER, filter))
                .thenReturn(new AuthAdminPageResponse<>(List.of(authUser), 0, 20, 1, 1));
        when(userProfileRepository.findByUserIdIn(List.of(USER_ID))).thenReturn(List.of(profile));
        when(authorRepository.findByUserProfileUserIdIn(List.of(USER_ID))).thenReturn(List.of(author));

        AdminPageResponse<AdminUserResponse> response = service.getUsers(AUTH_HEADER, filter);

        AdminUserResponse user = response.items().getFirst();
        assertThat(user.userId()).isEqualTo(USER_ID);
        assertThat(user.profile().profileId()).isEqualTo(PROFILE_ID);
        assertThat(user.authorProfile().authorId()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void addRoleDelegatesToAuthServiceAndMapsResponse() {
        AdminRoleRequest request = new AdminRoleRequest("admin");
        when(authAdminClient.addRole(AUTH_HEADER, USER_ID, request))
                .thenReturn(new AuthAdminRoleMutationResponse(USER_ID, List.of("user", "admin"), true));

        AdminRoleMutationResponse response = service.addRole(AUTH_HEADER, USER_ID, request);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.roles()).containsExactly("user", "admin");
        assertThat(response.changed()).isTrue();
        verify(authAdminClient).addRole(AUTH_HEADER, USER_ID, request);
    }

    private AuthAdminUserResponse authUser() {
        return new AuthAdminUserResponse(
                USER_ID,
                "user@example.test",
                List.of("user", "author"),
                true,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")
        );
    }

    private UserProfileEntity profile() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setId(PROFILE_ID);
        profile.setUserId(USER_ID);
        profile.setUsername("dev-user");
        return profile;
    }

    private AuthorEntity author(UserProfileEntity profile) {
        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(profile);
        author.setAuthorName("Backend Kitchen");
        return author;
    }
}
