package ec.edu.espe.tickets.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Verificacion de los JWT emitidos por el microservicio usuarios (RS256).
 *
 * <p>Este servicio solo VALIDA (no emite): posee unicamente la clave PUBLICA
 * (jwt.public-key), por lo que puede comprobar la firma de forma stateless pero
 * NO puede generar ni re-firmar tokens. Solo usuarios, dueno de la clave
 * privada, puede emitirlos.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";

    private final PublicKey publicKey;
    private final String issuer;

    public JwtService(
            @Value("${jwt.public-key}") String publicKeyMaterial,
            @Value("${jwt.issuer}") String issuer) {
        this.publicKey = RsaKeys.loadPublic(publicKeyMaterial);
        this.issuer = issuer;
    }

    /**
     * Verifica firma, emisor y expiracion, y exige que sea un ACCESS token
     * (un refresh token no autoriza peticiones). Lanza {@link JwtException} si
     * el token es invalido.
     */
    public Claims validarAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
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

    /**
     * Utilidad para cargar la clave PUBLICA RSA a partir de PEM. El "material"
     * puede ser una ruta a un archivo PEM o el contenido PEM codificado en base64.
     */
    static final class RsaKeys {

        private RsaKeys() {
        }

        static PublicKey loadPublic(String material) {
            try {
                byte[] der = Base64.getDecoder().decode(pemBody(readMaterial(material)));
                return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo cargar la clave publica RSA (jwt.public-key)", ex);
            }
        }

        private static String readMaterial(String material) throws IOException {
            String value = material == null ? "" : material.trim();
            Path path = Path.of(value);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }

        private static String pemBody(String pem) {
            return pem
                    .replaceAll("-----BEGIN [^-]+-----", "")
                    .replaceAll("-----END [^-]+-----", "")
                    .replaceAll("\\s", "");
        }
    }
}
