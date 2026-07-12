package ec.edu.espe.zonas.audit;

import lombok.Builder;
import lombok.Value;

/**
 * Espejo del CreateAuditEventDto de ms-audit. Los nombres de campo deben
 * coincidir exactamente (Jackson los serializa tal cual) porque ms-audit los
 * valida contra ese DTO al consumir el mensaje.
 */
@Value
@Builder
public class AuditEvent {
    String servicio;
    String accion;
    String entidad;
    Object datos;
    String usuario;
    String rol;
    String ip;
    String mac;
}
