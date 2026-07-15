package ec.edu.espe.usuarios.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/** Verifica la emision y validacion de access y refresh tokens y su separacion por tipo (RS256). */
class JwtServiceTest {

    private static final String ISSUER = "parqueadero";

    private JwtService jwtService;

    /** Genera un par RSA y devuelve el "material" (base64 del PEM) que espera el constructor. */
    private static String[] parMaterial() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        String priv = material(par.getPrivate().getEncoded(), "PRIVATE KEY");
        String pub = material(par.getPublic().getEncoded(), "PUBLIC KEY");
        return new String[] { priv, pub };
    }

    private static String material(byte[] der, String tipo) {
        String pem = "-----BEGIN " + tipo + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + tipo + "-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        String[] claves = parMaterial();
        // access = 120 min, refresh = 7 dias (10080 min).
        jwtService = new JwtService(claves[0], claves[1], ISSUER, 120, 10080);
    }

    @Test
    void accessTokenLlevaRolesYTipoAccess() {
        UUID id = UUID.randomUUID();
        String token = jwtService.generarToken(id, "mjacome", List.of("CLIENTE", "RECAUDADOR"));

        Claims claims = jwtService.validarTipo(token, JwtService.TYPE_ACCESS);

        assertEquals(id.toString(), claims.getSubject());
        assertEquals("mjacome", claims.get("username", String.class));
        assertEquals(JwtService.TYPE_ACCESS, claims.get(JwtService.CLAIM_TYPE, String.class));
        assertEquals(List.of("CLIENTE", "RECAUDADOR"), claims.get("roles", List.class));
    }

    @Test
    void refreshTokenNoLlevaRolesYEsTipoRefresh() {
        UUID id = UUID.randomUUID();
        String refresh = jwtService.generarRefreshToken(id, "mjacome");

        Claims claims = jwtService.validarTipo(refresh, JwtService.TYPE_REFRESH);

        assertEquals(id.toString(), claims.getSubject());
        assertEquals(JwtService.TYPE_REFRESH, claims.get(JwtService.CLAIM_TYPE, String.class));
        assertTrue(claims.get("roles") == null);
    }

    @Test
    void refreshTokenNoSirveComoAccess() {
        String refresh = jwtService.generarRefreshToken(UUID.randomUUID(), "mjacome");
        assertThrows(JwtException.class,
                () -> jwtService.validarTipo(refresh, JwtService.TYPE_ACCESS));
    }

    @Test
    void accessTokenNoSirveComoRefresh() {
        String access = jwtService.generarToken(UUID.randomUUID(), "mjacome", List.of("CLIENTE"));
        assertThrows(JwtException.class,
                () -> jwtService.validarTipo(access, JwtService.TYPE_REFRESH));
    }

    @Test
    void tokenConOtraFirmaEsRechazado() throws Exception {
        // Otro par de claves distinto: firma con SU privada, pero jwtService
        // valida con la publica original -> la firma no coincide y se rechaza.
        String[] otras = parMaterial();
        JwtService otro = new JwtService(otras[0], otras[1], ISSUER, 120, 10080);
        String ajeno = otro.generarToken(UUID.randomUUID(), "intruso", List.of("ROOT"));
        assertThrows(JwtException.class, () -> jwtService.validar(ajeno));
    }

    @Test
    void refreshDuraMasQueElAccess() {
        assertNotEquals(jwtService.getExpirationSeconds(), jwtService.getRefreshExpirationSeconds());
        assertTrue(jwtService.getRefreshExpirationSeconds() > jwtService.getExpirationSeconds());
    }
}
