package podcastService.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import podcastService.common.exception.UnauthorizedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthenticationServiceTest {

    private static final String SIGNING_KEY = "unit-test-signing-key";
    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final JwtAuthenticationService service = new JwtAuthenticationService(
            new JwtAuthenticationProperties(true, SIGNING_KEY, "auth-service"),
            new ObjectMapper()
    );

    @Test
    void parseAndValidateAcceptsValidAuthServiceToken() throws Exception {
        String token = token("""
                {"alg":"HS256","typ":"JWT"}
                """, """
                {"user_id":"%s","email":"user@example.com","roles":["user","author"],"iss":"auth-service","exp":%d}
                """.formatted(USER_ID, Instant.now().plusSeconds(60).getEpochSecond()), SIGNING_KEY);

        AuthenticatedUser user = service.parseAndValidate(token);

        assertThat(user.userId()).isEqualTo(USER_ID);
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.roles()).containsExactly("user", "author");
    }

    @Test
    void parseAndValidateRejectsExpiredToken() throws Exception {
        String token = token("""
                {"alg":"HS256","typ":"JWT"}
                """, """
                {"user_id":"%s","email":"user@example.com","roles":["user"],"iss":"auth-service","exp":%d}
                """.formatted(USER_ID, Instant.now().minusSeconds(1).getEpochSecond()), SIGNING_KEY);

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Access token is missing or expired");
    }

    @Test
    void parseAndValidateRejectsInvalidSignature() throws Exception {
        String token = token("""
                {"alg":"HS256","typ":"JWT"}
                """, """
                {"user_id":"%s","email":"user@example.com","roles":["user"],"iss":"auth-service","exp":%d}
                """.formatted(USER_ID, Instant.now().plusSeconds(60).getEpochSecond()), "wrong-unit-test-key");

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Access token signature is invalid");
    }

    @Test
    void parseAndValidateRejectsNonStringRole() throws Exception {
        String token = token("""
                {"alg":"HS256","typ":"JWT"}
                """, """
                {"user_id":"%s","email":"user@example.com","roles":["user",1],"iss":"auth-service","exp":%d}
                """.formatted(USER_ID, Instant.now().plusSeconds(60).getEpochSecond()), SIGNING_KEY);

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Access token roles claim is invalid");
    }

    private String token(String headerJson, String payloadJson, String secret) throws Exception {
        String header = encode(headerJson);
        String payload = encode(payloadJson);
        String signingInput = header + "." + payload;
        String signature = sign(signingInput, secret);
        return signingInput + "." + signature;
    }

    private String encode(String json) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
