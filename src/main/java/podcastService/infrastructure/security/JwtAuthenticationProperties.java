package podcastService.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.jwt")
public record JwtAuthenticationProperties(
        boolean enabled,
        String secret,
        String issuer
) {
}
