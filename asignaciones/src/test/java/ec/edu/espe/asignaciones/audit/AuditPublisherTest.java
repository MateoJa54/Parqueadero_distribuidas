package ec.edu.espe.asignaciones.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditPublisherTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private AuditPublisher buildPublisher() {
        return new AuditPublisher(mock(RabbitTemplate.class), "audit_exchange", "audit.event");
    }

    private AuditPublisher buildPublisherWithRabbit(RabbitTemplate rabbit) {
        return new AuditPublisher(rabbit, "audit_exchange", "audit.event");
    }

    // ----- publicar(3 args) sin contexto -----

    @Test
    void publicar_sinContexto_noLanzaExcepcion() {
        assertDoesNotThrow(() -> buildPublisher().publicar("CREATE", "ASIGNACION", "datos"));
    }

    // ----- publicar con usuario/rol explícitos -----

    @Test
    void publicar_conUsuarioExplicitoYRol_usaEsosValores() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        buildPublisherWithRabbit(rabbit).publicar("LOGIN", "SESION", "datos", "user@test.com", "ADMIN");
        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_usuarioExplicitoNull_infiereDesdePrincipal() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-abc", null,
                        List.of(new SimpleGrantedAuthority("ROLE_CONDUCTOR"))));

        buildPublisherWithRabbit(rabbit).publicar("UPDATE", "ASIGNACION", "datos", null, null);

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "uuid-abc".equals(((AuditEvent) obj).getUsuario())));
    }

    // ----- SecurityContext -----

    @Test
    void publicar_conContextoSeguridad_usaNombreDeAutenticacion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-123", null,
                        List.of(new SimpleGrantedAuthority("ROLE_RECAUDADOR"))));

        buildPublisherWithRabbit(rabbit).publicar("UPDATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_autenticacionAnonima_usuarioEsNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");
        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> ((AuditEvent) obj).getUsuario() == null));
    }

    // ----- IP -----

    @Test
    void publicar_conRequestConIP_extraeIP() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_conXForwardedFor_usaPrimeraIP() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_conIPv6Loopback_normalizaA127() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "127.0.0.1".equals(((AuditEvent) obj).getIp())));
    }

    @Test
    void publicar_conIPv6MappedLoopback_normalizaA127() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "127.0.0.1".equals(((AuditEvent) obj).getIp())));
    }

    @Test
    void publicar_conIPv4MapeadaEnIPv6_quitaPrefijo() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:192.168.1.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "192.168.1.5".equals(((AuditEvent) obj).getIp())));
    }

    // ----- MAC -----

    @Test
    void publicar_conMacHeader_usaMac() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Device-Mac", "AA:BB:CC:DD:EE:FF");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "AA:BB:CC:DD:EE:FF".equals(((AuditEvent) obj).getMac())));
    }

    @Test
    void publicar_sinMacHeader_usaValorPorDefecto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "00:00:00:00:00:00".equals(((AuditEvent) obj).getMac())));
    }

    // ----- JWT username claim -----

    @Test
    void publicar_conJwtConUsername_usaUsernameDelToken() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"uuid-123\",\"username\":\"juan.perez\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "juan.perez".equals(((AuditEvent) obj).getUsuario())));
    }

    @Test
    void publicar_conJwtSinUsernameEnPayload_caeAlUuidDelContexto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"uuid-abc\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-abc", null, List.of()));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"),
                argThat((Object obj) -> "uuid-abc".equals(((AuditEvent) obj).getUsuario())));
    }

    @Test
    void publicar_authorizationHeaderSinBearer_retornaNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_jwtConPayloadInvalido_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.valid.jwt!!!");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertDoesNotThrow(() -> buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos"));
    }

    @Test
    void publicar_jwtConSoloUnSegmento_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer solounasegmento");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertDoesNotThrow(() -> buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos"));
    }

    // ----- RabbitMQ failure -----

    @Test
    void publicar_rabbitLanzaExcepcion_noPropagaError() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new RuntimeException("rabbit down"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() ->
                buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos"));
    }

    // ----- username null en claim JWT -----

    @Test
    void publicar_jwtConUsernameNullEnClaim_caeAlContexto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"uuid-xyz\",\"username\":null}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-xyz", null, List.of()));

        buildPublisherWithRabbit(rabbit).publicar("DELETE", "ASIGNACION", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    // ----- con transaccion activa: difiere el envio hasta afterCommit -----

    @Test
    void publicar_conTransaccionActiva_enviaTrasCommit() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        TransactionSynchronizationManager.initSynchronization();
        try {
            buildPublisherWithRabbit(rabbit).publicar("CREATE", "ASIGNACION", "datos");

            // Aun no se envia: esta diferido hasta el commit.
            verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(AuditEvent.class));

            // Simula el commit: dispara afterCommit() de la sincronizacion registrada.
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
            verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
