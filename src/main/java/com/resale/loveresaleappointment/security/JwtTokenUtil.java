package com.resale.loveresaleappointment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {


    private static final Map<String, String> ISSUER_SECRETS = Map.of(
            "vso-auth", "MySuperSecureLongSecretKeyThatIsAtLeast32Bytes!",
            "user-ms", "AnotherVerySecureSecretKeyThatIs32PlusChars!"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey(token))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public Integer extractCustomerId(String token) {
        return extractClaim(token, claims -> claims.get("customerId", Integer.class));
    }

    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractPortal(String token) {
        return extractClaim(token, claims -> claims.get("portal", String.class));
    }

    private Key getSigningKey(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String issuer = objectMapper.readTree(payloadJson).get("iss").asText();

            String secret = ISSUER_SECRETS.get(issuer);
            if (secret == null) {
                throw new IllegalArgumentException("Unknown issuer: " + issuer);
            }

            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT token: " + e.getMessage(), e);
        }
    }
}


