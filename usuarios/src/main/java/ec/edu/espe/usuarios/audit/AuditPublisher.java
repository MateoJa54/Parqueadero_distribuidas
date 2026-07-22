package ec.edu.espe.usuarios.audit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Publica eventos de auditoria de este microservicio hacia ms-audit via
 * RabbitMQ. El usuario/rol se toman del JWT ya validado por
 * JwtAuthenticationFilter (SecurityContextHolder) y la ip/mac de la
 * peticion HTTP en curso (RequestContextHolder).
 *
 * <p>Nunca lanza excepcion: un fallo publicando el evento (RabbitMQ caido,
 * etc.) no debe interrumpir la operacion de negocio que lo origina.
 */
@Slf4j
@Component
public class AuditPublisher {

    private static final String SERVICIO = "ms-usuarios";
    private static final String IP_POR_DEFECTO = "127.0.0.1";
    private static final String MAC_POR_DEFECTO = "00:00:00:00:00:00";
    private static final String HEADER_MAC = "X-Device-Mac";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final ObjectMapper JWT_MAPPER = new ObjectMapper();

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public AuditPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${audit.exchange:audit_exchange}") String exchange,
            @Value("${audit.routing-key:audit.event}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(String accion, String entidad, Object datos) {
        publicar(accion, entidad, datos, null, null);
    }

    /**
     * Variante para casos donde el actor no se puede inferir del contexto de
     * seguridad -tipicamente LOGIN: en ese momento todavia no existe una
     * sesion autenticada, asi que usuarioActual()/rolActual() no tienen nada
     * que leer-. Si usuarioExplicito/rolExplicito vienen no-null, tienen
     * prioridad sobre lo inferido del JWT/SecurityContext.
     */
    public void publicar(String accion, String entidad, Object datos, String usuarioExplicito, String rolExplicito) {
        final AuditEvent evento;
        try {
            evento = AuditEvent.builder()
                    .servicio(SERVICIO)
                    .accion(accion)
                    .entidad(entidad)
                    .datos(datos)
                    .usuario(usuarioExplicito != null ? usuarioExplicito : usuarioActual())
                    .rol(rolExplicito != null ? rolExplicito : rolActual())
                    .ip(ipActual())
                    .mac(macActual())
                    .build();
        } catch (Exception ex) {
            log.warn("No se pudo construir el evento de auditoria [{} {}]: {}", accion, entidad, ex.getMessage());
            return;
        }

        Runnable envio = () -> {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            } catch (Exception ex) {
                log.warn("No se pudo publicar el evento de auditoria [{} {}]: {}", accion, entidad, ex.getMessage());
            }
        };

        // Diferir el envio a RabbitMQ hasta DESPUES del commit de la transaccion
        // de negocio: si la transaccion revierte, no se emite un evento fantasma
        // (elimina el dual-write). Sin transaccion activa (p.ej. LOGIN) se envia ya.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    /**
     * JwtAuthenticationFilter solo guarda el UUID (claim "sub") como
     * principal -a proposito, varios controllers dependen de que
     * authentication.getName() sea ese UUID- por lo que no sirve para
     * mostrar un usuario legible en la auditoria. Por eso el username se
     * lee directo del JWT (claim "username"), sin volver a validar la
     * firma: si llegamos hasta aqui, JwtAuthenticationFilter ya lo valido.
     * Si no se puede leer, se cae al UUID como respaldo.
     */
    private String usuarioActual() {
        String username = usernameDesdeToken();
        if (username != null) {
            return username;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private String usernameDesdeToken() {
        HttpServletRequest request = requestActual();
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            String[] partes = header.substring("Bearer ".length()).trim().split("\\.");
            if (partes.length < 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(partes[1]);
            JsonNode claims = JWT_MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
            JsonNode username = claims.get("username");
            return (username != null && !username.isNull()) ? username.asText() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String rolActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse(null);
    }

    private HttpServletRequest requestActual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private String ipActual() {
        HttpServletRequest request = requestActual();
        if (request == null) {
            return IP_POR_DEFECTO;
        }
        return normalizarIp(ipOrigenDeLaPeticion(request));
    }

    /**
     * Kong (y cualquier proxy delante del servicio) agrega el header
     * "X-Forwarded-For" con la IP real del cliente antes de reenviar la
     * peticion; sin esto, getRemoteAddr() solo veria la IP de Kong, no la
     * del usuario final. Si el header trae varias IPs separadas por coma
     * (cadena de proxies), la primera es la del cliente original.
     *
     * ADVERTENCIA: este header lo puede mandar cualquiera que llame
     * directo al microservicio (sin pasar por Kong), asi que no es una
     * fuente 100% confiable si el puerto del servicio queda expuesto sin
     * el gateway delante. Sirve como dato informativo, no como prueba forense.
     */
    private String ipOrigenDeLaPeticion(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return IP_POR_DEFECTO;
        }
        // Normaliza formas IPv6 de loopback comunes en desarrollo local, ya
        // que ms-audit valida estrictamente direcciones IPv4.
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return IP_POR_DEFECTO;
        }
        return ip.replaceFirst("^::ffff:", "");
    }

    private String macActual() {
        HttpServletRequest request = requestActual();
        if (request == null) {
            return MAC_POR_DEFECTO;
        }
        String mac = request.getHeader(HEADER_MAC);
        return (mac == null || mac.isBlank()) ? MAC_POR_DEFECTO : mac;
    }
}
