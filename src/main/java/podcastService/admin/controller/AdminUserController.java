package podcastService.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminRoleMutationResponse;
import podcastService.admin.dto.AdminRoleRequest;
import podcastService.admin.dto.AdminUserFilter;
import podcastService.admin.dto.AdminUserResponse;
import podcastService.admin.dto.AdminUserSort;
import podcastService.admin.service.AdminUserService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public AdminPageResponse<AdminUserResponse> getUsers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AdminUserSort sort
    ) {
        log.info("GET /admin/users, role={}, emailVerified={}, page={}, size={}",
                role, emailVerified, page, size);
        return adminUserService.getUsers(
                authorizationHeader,
                new AdminUserFilter(q, role, emailVerified, page, size, sort)
        );
    }

    @GetMapping("/{userId}")
    public AdminUserResponse getUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable UUID userId
    ) {
        log.info("GET /admin/users/{}", userId);
        return adminUserService.getUser(authorizationHeader, userId);
    }

    @PostMapping("/{userId}/roles")
    public AdminRoleMutationResponse addRole(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminRoleRequest request
    ) {
        log.info("POST /admin/users/{}/roles, roleName={}", userId, request.roleName());
        return adminUserService.addRole(authorizationHeader, userId, request);
    }

    @DeleteMapping("/{userId}/roles/admin")
    public AdminRoleMutationResponse removeAdminRole(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable UUID userId
    ) {
        log.info("DELETE /admin/users/{}/roles/admin", userId);
        return adminUserService.removeAdminRole(authorizationHeader, userId);
    }
}
