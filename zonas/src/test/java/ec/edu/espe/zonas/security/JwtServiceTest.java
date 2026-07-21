package ec.edu.espe.zonas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/** Verifica la validacion de access tokens RS256 emitidos por usuarios. */
class JwtServiceTest {

    private static final String ISSUER = "parqueadero";

    private JwtService jwtService;
    private PrivateKey privateKey;

    private static String materialPem(byte[] der, String tipo) {
        String pem = "-----BEGIN " + tipo + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + tipo + "-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair par = gen.generateKeyPair();
        privateKey = par.getPrivate();
        String pub = materialPem(par.getPublic().getEncoded(), "PUBLIC KEY");
        jwtService = new JwtService(pub, ISSUER);
    }

    private String tokenConTipo(String tipo) {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("uuid-123")
                .claim("type", tipo)
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void validarAccessTokenValido() {
        Claims claims = jwtService.validarAccessToken(tokenConTipo("access"));
        assertEquals("uuid-123", claims.getSubject());
        assertEquals(List.of("ADMIN"), claims.get("roles", List.class));
    }

    @Test
    void refreshTokenNoSirveComoAccess() {
        String refresh = tokenConTipo("refresh");
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(refresh));
    }

    @Test
    void tokenConOtroEmisorEsRechazado() {
        String ajeno = Jwts.builder()
                .issuer("otro-emisor")
                .subject("x")
                .claim("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(ajeno));
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        PrivateKey otra = gen.generateKeyPair().getPrivate();
        String ajeno = Jwts.builder()
                .issuer(ISSUER)
                .subject("x")
                .claim("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otra, Jwts.SIG.RS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.validarAccessToken(ajeno));
    }

    @Test
    void materialInvalidoLanzaIllegalState() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("no-es-una-clave-valida", ISSUER));
    }

    @Test
    void constructorAceptaMaterialConEncabezadosPem() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair par = gen.generateKeyPair();
        // Material base64 de un PEM con encabezados BEGIN/END: debe cargarse bien.
        String pub = materialPem(par.getPublic().getEncoded(), "PUBLIC KEY");
        JwtService svc = new JwtService(pub, ISSUER);

        String token = Jwts.builder()
                .issuer(ISSUER).subject("s").claim("type", "access")
                .claims(Map.of())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(par.getPrivate(), Jwts.SIG.RS256).compact();
        assertEquals("s", svc.validarAccessToken(token).getSubject());
    }
}
