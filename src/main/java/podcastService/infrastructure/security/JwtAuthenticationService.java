package podcastService.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import podcastService.common.exception.UnauthorizedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final JwtAuthenticationProperties properties;
    private final ObjectMapper objectMapper;

    public AuthenticatedUser parseAndValidate(String token) {
        if (!properties.enabled()) {
            throw new UnauthorizedException("Authentication is disabled");
        }

        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new UnauthorizedException("Authentication is not configured");
        }

        String[] parts = token == null ? new String[0] : token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new UnauthorizedException("Access token is invalid");
        }

        JsonNode header = decodeJson(parts[0], "Access token header is invalid");
        JsonNode payload = decodeJson(parts[1], "Access token payload is invalid");

        String algorithm = textClaim(header, "alg");
        if (!"HS256".equals(algorithm)) {
            throw new UnauthorizedException("Access token signing algorithm is not supported");
        }

        verifySignature(parts[0] + "." + parts[1], parts[2]);
        validateRegisteredClaims(payload);

        UUID userId = uuidClaim(payload, "user_id");
        String email = textClaim(payload, "email");
        List<String> roles = rolesClaim(payload);

        return new AuthenticatedUser(userId, email, roles);
    }

    private JsonNode decodeJson(String encoded, String errorMessage) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readTree(decoded);
        } catch (Exception exception) {
            throw new UnauthorizedException(errorMessage);
        }
    }

    private void verifySignature(String signingInput, String encodedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] expected = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getUrlDecoder().decode(encodedSignature);

            if (!MessageDigest.isEqual(expected, actual)) {
                throw new UnauthorizedException("Access token signature is invalid");
            }
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("Access token signature is invalid");
        }
    }

    private void validateRegisteredClaims(JsonNode payload) {
        String expectedIssuer = properties.issuer();
        if (expectedIssuer != null && !expectedIssuer.isBlank()) {
            String issuer = textClaim(payload, "iss");
            if (!expectedIssuer.equals(issuer)) {
                throw new UnauthorizedException("Access token issuer is invalid");
            }
        }

        JsonNode expiresAt = payload.get("exp");
        if (expiresAt == null || !expiresAt.canConvertToLong()) {
            throw new UnauthorizedException("Access token expiration is missing");
        }

        Instant now = Instant.now();
        if (Instant.ofEpochSecond(expiresAt.asLong()).plusSeconds(CLOCK_SKEW_SECONDS).isBefore(now)) {
            throw new UnauthorizedException("Access token is missing or expired");
        }

        JsonNode notBefore = payload.get("nbf");
        if (notBefore != null) {
            if (!notBefore.canConvertToLong()) {
                throw new UnauthorizedException("Access token not-before claim is invalid");
            }

            if (Instant.ofEpochSecond(notBefore.asLong()).minusSeconds(CLOCK_SKEW_SECONDS).isAfter(now)) {
                throw new UnauthorizedException("Access token is not active yet");
            }
        }
    }

    private UUID uuidClaim(JsonNode payload, String claimName) {
        String value = textClaim(payload, claimName);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Access token " + claimName + " claim is invalid");
        }
    }

    private String textClaim(JsonNode node, String claimName) {
        JsonNode value = node.get(claimName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new UnauthorizedException("Access token " + claimName + " claim is invalid");
        }
        return value.asText();
    }

    private List<String> rolesClaim(JsonNode payload) {
        JsonNode rolesNode = payload.get("roles");
        if (rolesNode == null || !rolesNode.isArray()) {
            return List.of();
        }

        List<String> roles = new ArrayList<>();
        for (JsonNode roleNode : rolesNode) {
            if (!roleNode.isTextual() || roleNode.asText().isBlank()) {
                throw new UnauthorizedException("Access token roles claim is invalid");
            }
            roles.add(roleNode.asText());
        }
        return List.copyOf(roles);
    }
}
