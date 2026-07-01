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

    /** Nombre del claim que distingue el proposito del token. */
    public static final String CLAIM_TYPE = "type";
    /** Access token: corto, transporta roles y autoriza peticiones. */
    public static final String TYPE_ACCESS = "access";
    /** Refresh token: largo, sin roles, solo sirve para pedir un nuevo access. */
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final String issuer;
    private final long expirationMinutes;
    private final long refreshExpirationMinutes;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration-minutes}") long expirationMinutes,
            @Value("${jwt.refresh-expiration-minutes}") long refreshExpirationMinutes) {
        // HS256 exige una clave de al menos 256 bits (32 bytes). El secreto por
        // defecto cumple ese tamano; si se acorta por env, fallara al arrancar.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
        this.refreshExpirationMinutes = refreshExpirationMinutes;
    }

    /** Genera el ACCESS token (corto) para un usuario con sus roles activos. */
    public String generarToken(UUID idUsuario, String username, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(idUsuario.toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
    }

    /**
     * Genera el REFRESH token (largo). No lleva roles: solo identifica al usuario
     * para que pueda solicitar un nuevo access token sin volver a autenticarse.
     */
    public String generarRefreshToken(UUID idUsuario, String username) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(refreshExpirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(idUsuario.toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim("username", username)
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

    /**
     * Valida un token y exige que sea del tipo indicado. Evita, por ejemplo, que
     * un refresh token se use como access token (o viceversa).
     */
    public Claims validarTipo(String token, String tipoEsperado) {
        Claims claims = validar(token);
        String tipo = claims.get(CLAIM_TYPE, String.class);
        if (!tipoEsperado.equals(tipo)) {
            throw new io.jsonwebtoken.JwtException(
                    "Tipo de token invalido: se esperaba '" + tipoEsperado + "'");
        }
        return claims;
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMinutes * 60;
    }
}
