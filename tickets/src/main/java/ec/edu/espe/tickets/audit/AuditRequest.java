package ec.edu.espe.tickets.audit;

/**
 * Evento de dominio interno que solicita registrar una entrada de auditoria.
 *
 * <p>Se publica con {@code ApplicationEventPublisher} DENTRO de la transaccion
 * de negocio, pero el envio real a RabbitMQ ocurre solo despues del commit
 * (ver {@link AuditEventListener}). Asi se evita el problema de "dual-write":
 * si la transaccion termina en rollback, nunca se emite un evento fantasma.
 *
 * @param accion  operacion realizada (CREATE, UPDATE, DELETE, LOGIN, ...).
 * @param entidad entidad afectada (ej. TICKET).
 * @param datos   snapshot del registro afectado.
 */
public record AuditRequest(String accion, String entidad, Object datos) {
}
