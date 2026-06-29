package ec.edu.espe.usuarios.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emision y verificacion de JSON Web Tokens firmados con HS256.
 *
 * El token es auto-contenido (stateless): transporta el id del usuario, su
 * username y la lista de roles activos. Cualquier microservicio que comparta el
 * mismo {@code jwt.secret} puede validarlo sin consultar a usuarios.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        // HS256 exige una clave de al menos 256 bits (32 bytes). El secreto por
        // defecto cumple ese tamano; si se acorta por env, fallara al arrancar.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    /** Genera un token para un usuario con sus roles activos. */
    public String generarToken(UUID idUsuario, String username, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(idUsuario.toString())
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
    }

    /** Verifica la firma y la expiracion, y devuelve los claims. Lanza excepcion si es invalido. */
    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
