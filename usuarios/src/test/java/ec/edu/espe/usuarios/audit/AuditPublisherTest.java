package ec.edu.espe.usuarios.audit;

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

    @Test
    void publicar_sinContexto_noLanzaExcepcion() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        AuditPublisher publisher = buildPublisher();
        assertDoesNotThrow(() -> publisher.publicar("CREATE", "USUARIO", "datos"));
    }

    @Test
    void publicar_conUsuarioExplicitoYRol_usaEsosValores() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("LOGIN", "SESION", "datos", "user@test.com", "ADMIN");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
    }

    @Test
    void publicar_conContextoSeguridad_usaNombreDeAutenticacion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "uuid-123", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("UPDATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicar_conRequestConIP_extraeIP() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conXForwardedFor_usaPrimeraIP() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conIPv6Loopback_normalizaA127() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "127.0.0.1".equals(ev.getIp());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conIPv6MappedLoopback_normalizaA127() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "127.0.0.1".equals(ev.getIp());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conIPv4MapeadaEnIPv6_quitaPrefijo() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:192.168.1.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "192.168.1.5".equals(ev.getIp());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conMacHeader_usaMac() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Device-Mac", "AA:BB:CC:DD:EE:FF");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "AA:BB:CC:DD:EE:FF".equals(ev.getMac());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_sinMacHeader_usaValorPorDefecto() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "00:00:00:00:00:00".equals(ev.getMac());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conJwtConUsername_usaUsernameDelToken() throws Exception {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"uuid-123\",\"username\":\"juan.perez\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "juan.perez".equals(ev.getUsuario());
        }));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conJwtSinUsernameEnPayload_caeAlUuidDelContexto() throws Exception {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"uuid-abc\"}".getBytes());
        String fakeJwt = header + "." + payload + ".fakesig";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + fakeJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uuid-abc", null, List.of()));

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return "uuid-abc".equals(ev.getUsuario());
        }));

        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicar_authorizationHeaderSinBearer_retornaNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(AuditEvent.class));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_jwtConPayloadInvalido_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.valid.jwt!!!");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "USUARIO", "datos"));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_rabbitLanzaExcepcion_noPropagaError() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new RuntimeException("rabbit down"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));

        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "USUARIO", "datos"));
    }

    @Test
    void publicar_autenticacionAnonima_usuarioEsNull() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        publisher.publicar("CREATE", "USUARIO", "datos");

        verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), argThat((Object obj) -> {
            AuditEvent ev = (AuditEvent) obj;
            return ev.getUsuario() == null;
        }));
    }

    @Test
    void publicar_jwtConSoloUnSegmento_noPropagaExcepcion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer solounasegmento");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> publisher.publicar("CREATE", "USUARIO", "datos"));

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void publicar_conTransaccionActiva_enviaTrasCommit() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        AuditPublisher publisher = buildPublisherWithRabbit(rabbit);

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publicar("CREATE", "USUARIO", "datos");

            verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
            verify(rabbit).convertAndSend(eq("audit_exchange"), eq("audit.event"), any(Object.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
