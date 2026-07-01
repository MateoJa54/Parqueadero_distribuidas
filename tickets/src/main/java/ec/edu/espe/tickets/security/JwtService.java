package ec.edu.espe.tickets.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Verificacion de los JWT emitidos por el microservicio usuarios.
 *
 * <p>Este servicio solo VALIDA (no emite): comparte el mismo {@code jwt.secret}
 * e {@code issuer} que usuarios, por lo que puede autenticar peticiones de forma
 * stateless sin llamar a usuarios en cada request.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    /**
     * Verifica firma, emisor y expiracion, y exige que sea un ACCESS token
     * (un refresh token no autoriza peticiones). Lanza {@link JwtException} si
     * el token es invalido.
     */
    public Claims validarAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tipo = claims.get(CLAIM_TYPE, String.class);
        if (!TYPE_ACCESS.equals(tipo)) {
            throw new JwtException("Se esperaba un access token");
        }
        return claims;
    }
}
