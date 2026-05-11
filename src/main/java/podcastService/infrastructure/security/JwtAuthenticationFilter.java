package podcastService.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import podcastService.common.exception.UnauthorizedException;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationProperties properties;
    private final JwtAuthenticationService authenticationService;
    private final SecurityErrorResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled() || isInfrastructurePath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token;
        try {
            token = resolveBearerToken(authorization);
        } catch (UnauthorizedException exception) {
            responseWriter.writeUnauthorized(response, exception.getMessage());
            return;
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser authenticatedUser = authenticationService.parseAndValidate(token);
            log.debug(
                    "JWT authenticated: method={}, path={}, userId={}, roles={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    authenticatedUser.userId(),
                    authenticatedUser.roles()
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    token,
                    authenticatedUser.roles().stream()
                            .map(this::toAuthority)
                            .toList()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            SecurityContextHolder.clearContext();
            log.warn(
                    "JWT authentication failed: method={}, path={}, reason={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage()
            );
            responseWriter.writeUnauthorized(response, exception.getMessage());
        }
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new UnauthorizedException("Authorization header must use Bearer scheme");
        }

        String token = authorization.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean isInfrastructurePath(HttpServletRequest request) {
        String path = normalizePath(request);
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/openapi")
                || path.equals("/error");
    }

    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private SimpleGrantedAuthority toAuthority(String role) {
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            return new SimpleGrantedAuthority(normalizedRole);
        }
        return new SimpleGrantedAuthority("ROLE_" + normalizedRole);
    }
}
