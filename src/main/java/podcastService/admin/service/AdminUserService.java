package podcastService.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.admin.client.AuthAdminClient;
import podcastService.admin.client.dto.AuthAdminPageResponse;
import podcastService.admin.client.dto.AuthAdminUserResponse;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminRoleMutationResponse;
import podcastService.admin.dto.AdminRoleRequest;
import podcastService.admin.dto.AdminUserFilter;
import podcastService.admin.dto.AdminUserResponse;
import podcastService.admin.mapper.AdminMapper;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AuthAdminClient authAdminClient;
    private final UserProfileRepository userProfileRepository;
    private final AuthorRepository authorRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminUserResponse> getUsers(String authorizationHeader, AdminUserFilter filter) {
        AuthAdminPageResponse<AuthAdminUserResponse> authPage = authAdminClient.getUsers(authorizationHeader, filter);
        Collection<UUID> userIds = authPage.items().stream()
                .map(AuthAdminUserResponse::userId)
                .filter(userId -> userId != null)
                .toList();
        Map<UUID, UserProfileEntity> profilesByUserId = findProfilesByUserId(userIds);
        Map<UUID, AuthorEntity> authorsByUserId = findAuthorsByUserId(userIds);

        return new AdminPageResponse<>(
                authPage.items().stream()
                        .map(user -> adminMapper.toUserResponse(user, profilesByUserId, authorsByUserId))
                        .toList(),
                authPage.page(),
                authPage.size(),
                authPage.totalElements(),
                authPage.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(String authorizationHeader, UUID userId) {
        AuthAdminUserResponse authUser = authAdminClient.getUser(authorizationHeader, userId);
        Map<UUID, UserProfileEntity> profilesByUserId = findProfilesByUserId(java.util.List.of(userId));
        Map<UUID, AuthorEntity> authorsByUserId = findAuthorsByUserId(java.util.List.of(userId));

        return adminMapper.toUserResponse(authUser, profilesByUserId, authorsByUserId);
    }

    public AdminRoleMutationResponse addRole(
            String authorizationHeader,
            UUID userId,
            AdminRoleRequest request
    ) {
        return adminMapper.toRoleMutationResponse(authAdminClient.addRole(authorizationHeader, userId, request));
    }

    public AdminRoleMutationResponse removeAdminRole(String authorizationHeader, UUID userId) {
        return adminMapper.toRoleMutationResponse(authAdminClient.removeAdminRole(authorizationHeader, userId));
    }

    private Map<UUID, UserProfileEntity> findProfilesByUserId(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserProfileEntity::getUserId, Function.identity()));
    }

    private Map<UUID, AuthorEntity> findAuthorsByUserId(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return authorRepository.findByUserProfileUserIdIn(userIds).stream()
                .collect(Collectors.toMap(author -> author.getUserProfile().getUserId(), Function.identity()));
    }
}
