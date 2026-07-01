package ec.edu.espe.usuarios.security;

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

/** Verifica la emision y validacion de access y refresh tokens y su separacion por tipo. */
class JwtServiceTest {

    private static final String SECRET = "parqueadero-espe-clave-secreta-jwt-cambia-esto-en-produccion-2026";
    private static final String ISSUER = "parqueadero";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // access = 120 min, refresh = 7 dias (10080 min).
        jwtService = new JwtService(SECRET, ISSUER, 120, 10080);
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
    void tokenConOtraFirmaEsRechazado() {
        JwtService otro = new JwtService(
                "otra-clave-secreta-distinta-de-al-menos-256-bits-para-hs256-000", ISSUER, 120, 10080);
        String ajeno = otro.generarToken(UUID.randomUUID(), "intruso", List.of("ROOT"));
        assertThrows(JwtException.class, () -> jwtService.validar(ajeno));
    }

    @Test
    void refreshDuraMasQueElAccess() {
        assertNotEquals(jwtService.getExpirationSeconds(), jwtService.getRefreshExpirationSeconds());
        assertTrue(jwtService.getRefreshExpirationSeconds() > jwtService.getExpirationSeconds());
    }
}
