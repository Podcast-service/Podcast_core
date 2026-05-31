package podcastService.author.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import podcastService.author.dto.BecomeAuthorResponse;
import podcastService.author.dto.CreateAuthorProfileRequest;
import podcastService.auth.client.AuthRoleClient;
import podcastService.auth.dto.AuthRoleUpdateResponse;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BecomeAuthorService {

    private static final String AUTHOR_ROLE = "author";

    private final AuthRoleClient authRoleClient;
    private final AuthorProfileService authorProfileService;

    public BecomeAuthorResult becomeAuthor(
            UUID currentUserId,
            String authorizationHeader,
            CreateAuthorProfileRequest request
    ) {
        log.info("Author onboarding started, userId={}", currentUserId);
        AuthRoleUpdateResponse tokenResponse = authRoleClient.addRole(authorizationHeader, AUTHOR_ROLE);
        AuthorProfileCreationResult profileResult = authorProfileService.createOrGet(currentUserId, request);

        log.info(
                "Author onboarding completed, userId={}, authorProfileCreated={}",
                currentUserId,
                profileResult.created()
        );
        return new BecomeAuthorResult(
                new BecomeAuthorResponse(
                        tokenResponse.accessToken(),
                        tokenResponse.expiresIn(),
                        profileResult.profile()
                ),
                profileResult.created()
        );
    }
}
