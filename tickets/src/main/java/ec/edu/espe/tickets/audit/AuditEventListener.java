package ec.edu.espe.tickets.audit;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Envia a RabbitMQ los eventos de auditoria SOLO cuando la transaccion de
 * negocio que los origino se confirmo (AFTER_COMMIT).
 *
 * <ul>
 *   <li>Elimina el "dual-write": un rollback ya no puede dejar un evento de
 *       auditoria publicado para una operacion que en realidad no ocurrio.</li>
 *   <li>Corre de forma sincrona en el MISMO hilo de la peticion, por lo que el
 *       {@code SecurityContext} y el {@code RequestContext} (usuario/ip/mac)
 *       siguen disponibles para {@link AuditPublisher}.</li>
 *   <li>{@code fallbackExecution = true}: si por alguna razon se publica fuera
 *       de una transaccion, el evento igual se envia (no se pierde).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditPublisher auditPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alConfirmar(AuditRequest evento) {
        auditPublisher.publicar(evento.accion(), evento.entidad(), evento.datos());
    }
}
