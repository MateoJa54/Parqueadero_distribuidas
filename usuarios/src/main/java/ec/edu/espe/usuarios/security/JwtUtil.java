package ec.edu.espe.usuarios.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.security.Key;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private Key signingKey;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(properties.getExpirationSeconds())))
                .claim("userId", userId.toString())
                .claim("roles", roles)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(claims);
        } catch (SecurityException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !isTokenExpired(claims);
        } catch (SecurityException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        String userId = parseClaims(token).get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    public LocalDateTime getExpirationAt(String token) {
        return parseClaims(token).getExpiration().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private Claims parseClaims(String token) {
        Jws<Claims> parsed = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
        return parsed.getBody();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(Date.from(Instant.now()));
    }
}
