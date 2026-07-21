package ec.edu.espe.asignaciones.security;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Verifica la validacion stateless de access tokens (RS256) y la carga de la
 * clave publica desde material base64 o desde ruta a archivo PEM.
 */
class JwtServiceTest {

    private static final String ISSUER = "parqueadero";

    private PrivateKey privateKey;
    private String publicMaterial;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        this.privateKey = par.getPrivate();
        this.publicMaterial = material(par.getPublic().getEncoded(), "PUBLIC KEY");
        this.jwtService = new JwtService(publicMaterial, ISSUER);
    }

    private static String material(byte[] der, String tipo) {
        String pem = "-----BEGIN " + tipo + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + tipo + "-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private String firmar(String type, PrivateKey key, String iss) {
        var builder = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuer(iss)
                .claim("roles", List.of("CONDUCTOR"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 120_000));
        if (type != null) {
            builder.claim("type", type);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void validarAccessToken_tokenValido_devuelveClaims() {
        String token = firmar("access", privateKey, ISSUER);

        Claims claims = jwtService.validarAccessToken(token);

        assertNotNull(claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void validarAccessToken_tokenSinTipoAccess_lanzaJwtException() {
        String refresh = firmar("refresh", privateKey, ISSUER);
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(refresh));
    }

    @Test
    void validarAccessToken_tokenSinClaimType_lanzaJwtException() {
        String sinTipo = firmar(null, privateKey, ISSUER);
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(sinTipo));
    }

    @Test
    void validarAccessToken_emisorIncorrecto_lanzaJwtException() {
        String token = firmar("access", privateKey, "otro-emisor");
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(token));
    }

    @Test
    void validarAccessToken_firmaAjena_lanzaJwtException() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        PrivateKey otra = generador.generateKeyPair().getPrivate();
        String ajeno = firmar("access", otra, ISSUER);
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(ajeno));
    }

    @Test
    void validarAccessToken_tokenBasura_lanzaExcepcion() {
        assertThrows(RuntimeException.class, () -> jwtService.validarAccessToken("no.es.un.jwt"));
    }

    @Test
    void constructor_materialDesdeRutaPem_cargaClavePublica(@TempDir Path dir) throws Exception {
        String pem = new String(Base64.getDecoder().decode(publicMaterial), StandardCharsets.UTF_8);
        Path pemFile = dir.resolve("public.pem");
        Files.writeString(pemFile, pem, StandardCharsets.UTF_8);

        JwtService desdeArchivo = new JwtService(pemFile.toString(), ISSUER);
        String token = firmar("access", privateKey, ISSUER);

        assertNotNull(desdeArchivo.validarAccessToken(token));
    }

    @Test
    void constructor_materialInvalido_lanzaIllegalState() {
        assertThrows(IllegalStateException.class, () -> new JwtService("%%%no-base64%%%", ISSUER));
    }

    @Test
    void constructor_materialNull_lanzaIllegalState() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, ISSUER));
    }
}
