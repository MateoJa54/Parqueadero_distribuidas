package ec.edu.espe.usuarios.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * Emision y verificacion de JSON Web Tokens firmados con RS256 (asimetrico).
 *
 * <p>usuarios es la UNICA autoridad que posee la clave PRIVADA y, por tanto, el
 * unico servicio capaz de FIRMAR tokens. Los demas microservicios solo reciben
 * la clave PUBLICA y unicamente pueden VERIFICAR la firma. Asi, aunque el codigo
 * o la clave publica se hagan visibles, nadie puede re-firmar un token (por
 * ejemplo en jwt.io) cambiando sus roles: la firma no coincidiria sin la clave
 * privada. Esto elimina la escalada de privilegios por falsificacion de tokens.
 *
 * <p>El token es auto-contenido (stateless): transporta el id del usuario, su
 * username y la lista de roles activos.
 */
@Service
public class JwtService {

    /** Nombre del claim que distingue el proposito del token. */
    public static final String CLAIM_TYPE = "type";
    /** Access token: corto, transporta roles y autoriza peticiones. */
    public static final String TYPE_ACCESS = "access";
    /** Refresh token: largo, sin roles, solo sirve para pedir un nuevo access. */
    public static final String TYPE_REFRESH = "refresh";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String issuer;
    private final long expirationMinutes;
    private final long refreshExpirationMinutes;

    public JwtService(
            @Value("${jwt.private-key}") String privateKeyMaterial,
            @Value("${jwt.public-key}") String publicKeyMaterial,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.expiration-minutes}") long expirationMinutes,
            @Value("${jwt.refresh-expiration-minutes}") long refreshExpirationMinutes) {
        this.privateKey = RsaKeys.loadPrivate(privateKeyMaterial);
        this.publicKey = RsaKeys.loadPublic(publicKeyMaterial);
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
                .signWith(privateKey, Jwts.SIG.RS256)
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
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /** Verifica la firma y la expiracion, y devuelve los claims. Lanza excepcion si es invalido. */
    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
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

    /**
     * Utilidad para cargar claves RSA a partir de PEM. El "material" puede ser:
     * <ul>
     *   <li>una ruta a un archivo PEM (por ejemplo {@code keys/jwt_private.pem}), o</li>
     *   <li>el contenido PEM codificado en base64 (util en contenedores/variables de entorno).</li>
     * </ul>
     */
    static final class RsaKeys {

        private RsaKeys() {
        }

        static PrivateKey loadPrivate(String material) {
            try {
                byte[] der = Base64.getDecoder().decode(pemBody(readMaterial(material)));
                return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo cargar la clave privada RSA (jwt.private-key)", ex);
            }
        }

        static PublicKey loadPublic(String material) {
            try {
                byte[] der = Base64.getDecoder().decode(pemBody(readMaterial(material)));
                return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            } catch (Exception ex) {
                throw new IllegalStateException("No se pudo cargar la clave publica RSA (jwt.public-key)", ex);
            }
        }

        /** Lee el archivo si {@code material} es una ruta existente; si no, lo trata como base64 del PEM. */
        private static String readMaterial(String material) throws IOException {
            String value = material == null ? "" : material.trim();
            Path path = Path.of(value);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }

        /** Extrae el cuerpo base64 de un PEM, quitando cabeceras y saltos de linea. */
        private static String pemBody(String pem) {
            return pem
                    .replaceAll("-----BEGIN [^-]+-----", "")
                    .replaceAll("-----END [^-]+-----", "")
                    .replaceAll("\\s", "");
        }
    }
}
