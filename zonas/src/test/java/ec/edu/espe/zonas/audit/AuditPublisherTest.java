package ec.edu.espe.zonas.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditPublisherTest {

    private AuditPublisher buildPublisher() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        return new AuditPublisher(rabbit, "audit_exchange", "audit.event");
    }

    private AuditPublisher buildPublisherWithRabbit(RabbitTemplate rabbit) {
        return new AuditPublisher(rabbit, "audit_exchange", "audit.event");
    }

    // -----------------------------------------------------------------------
    // 1. publicar 3-arg (delegates to 5-arg with nulls)
    // -----------------------------------------------------------------------

    @Test
    void publicar_3arg_sinContexto_noLanzaExcepcion() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        AuditPublisher publisher = buildPublisher();
        assertDoesNotThrow(() -> publisher.publicar("CREATE", "ZONA", "datos"));
    }

    // -----------------------------------------------------------------------
    // 2. publicar 5-arg: usuario y rol explícitos tienen prioridad
    // -----------------------------------------------------------------------

    @Test
    void publicar_conUsuarioExplicitoYRol_eventoTieneEsosValores() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("LOGIN", "SESION", "datos", "operador@espe.edu.ec", "OPERADOR");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "operador@espe.edu.ec".equals(ev.getUsuario()) && "OPERADOR".equals(ev.getRol());
        }));
    }

    // -----------------------------------------------------------------------
    // 3. SecurityContext sin JWT → usa getName() del Authentication
    // -----------------------------------------------------------------------

    @Test
    void publicar_conContextoSeguridad_usaNombreDeAutenticacion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "uuid-123", null,
                        List.of(new SimpleGrantedAuthority("ROLE_RECAUDADOR"))));

        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("UPDATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // 4. rol con prefijo ROLE_ es recortado
    // -----------------------------------------------------------------------

    @Test
    void publicar_rolConPrefijoROLE_esEliminado() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "uuid-xyz", null,
                        List.of(new SimpleGrantedAuthority("ROLE_OPERADOR"))));
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("READ", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "OPERADOR".equals(ev.getRol());
        }));

        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // 5,7,8,9. IP desde remoteAddr con normalizacion (parametrizado)
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "10.0.0.5,          10.0.0.5",
            "::1,               127.0.0.1",
            "0:0:0:0:0:0:0:1,   127.0.0.1",
            "::ffff:192.168.1.5, 192.168.1.5"
    })
    void publicar_remoteAddr_normalizaIP(String remoteAddr, String esperada) {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return esperada.equals(ev.getIp());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 6. IP: X-Forwarded-For con múltiples IPs → primera
    // -----------------------------------------------------------------------

    @Test
    void publicar_conXForwardedFor_usaPrimeraIP() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "203.0.113.1".equals(ev.getIp());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 10. IP: sin request → 127.0.0.1 por defecto
    // -----------------------------------------------------------------------

    @Test
    void publicar_sinRequest_usaIPPorDefecto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "127.0.0.1".equals(ev.getIp());
        }));
    }

    // -----------------------------------------------------------------------
    // 11. MAC: header X-Device-Mac presente
    // -----------------------------------------------------------------------

    @Test
    void publicar_conMacHeader_usaMac() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Device-Mac", "AA:BB:CC:DD:EE:FF");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "AA:BB:CC:DD:EE:FF".equals(ev.getMac());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 12. MAC: sin header → 00:00:00:00:00:00 (con request presente)
    // -----------------------------------------------------------------------

    @Test
    void publicar_sinMacHeader_usaValorPorDefecto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "00:00:00:00:00:00".equals(ev.getMac());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 13. MAC: sin request → 00:00:00:00:00:00 por defecto
    // -----------------------------------------------------------------------

    @Test
    void publicar_sinRequest_usaMacPorDefecto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("DELETE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "00:00:00:00:00:00".equals(ev.getMac());
        }));
    }

    // -----------------------------------------------------------------------
    // 14. JWT con claim "username" → lo usa como usuario
    // -----------------------------------------------------------------------

    @Test
    void publicar_conJwtConUsername_usaUsernameDelToken() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        String header  = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"uuid-123\",\"username\":\"carlos.perez\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "carlos.perez".equals(ev.getUsuario());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 15. JWT sin "username" en payload → cae al UUID del SecurityContext
    // -----------------------------------------------------------------------

    @Test
    void publicar_conJwtSinUsernameEnPayload_caeAlUuidDelContexto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        String header  = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"uuid-abc\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-abc", null, List.of()));

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "uuid-abc".equals(ev.getUsuario());
        }));

        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // 16. Authorization header sin prefijo "Bearer " → usernameDesdeToken null
    // -----------------------------------------------------------------------

    @Test
    void publicar_authorizationHeaderSinBearer_retornaNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 17. JWT malformado (payload no decodificable) → no propaga excepción
    // -----------------------------------------------------------------------

    @Test
    void publicar_jwtConPayloadInvalido_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.valid.jwt!!!");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "ZONA", "datos"));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 18. JWT con un solo segmento (sin punto) → retorna null sin excepción
    // -----------------------------------------------------------------------

    @Test
    void publicar_jwtConSoloUnSegmento_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer solounasegmento");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "ZONA", "datos"));

        RequestContextHolder.resetRequestAttributes();
    }

    // -----------------------------------------------------------------------
    // 19. RabbitMQ lanza excepción → se traga el error
    // -----------------------------------------------------------------------

    @Test
    void publicar_rabbitLanzaExcepcion_noPropagaError() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new RuntimeException("rabbit down"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "ZONA", "datos"));
    }

    // -----------------------------------------------------------------------
    // 20. Sin SecurityContext ni request → usuario es null
    // -----------------------------------------------------------------------

    @Test
    void publicar_autenticacionAnonima_usuarioEsNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("CREATE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return ev.getUsuario() == null;
        }));
    }

    // -----------------------------------------------------------------------
    // 21. Principal == "anonymousUser" literal → usuario es null
    // -----------------------------------------------------------------------

    @Test
    void publicar_principalAnonymousUser_usuarioEsNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("READ", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return ev.getUsuario() == null;
        }));

        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // 22. Servicio siempre es "ms-zonas"
    // -----------------------------------------------------------------------

    @Test
    void publicar_eventoSiempreTieneServicioMsZonas() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("DELETE", "ZONA", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "ms-zonas".equals(ev.getServicio());
        }));
    }
}
