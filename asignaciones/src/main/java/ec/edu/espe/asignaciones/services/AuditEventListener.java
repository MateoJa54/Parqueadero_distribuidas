package ec.edu.espe.asignaciones.services;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ec.edu.espe.asignaciones.audit.AuditPublisher;
import ec.edu.espe.asignaciones.entities.AssignmentAuditEvent;
import ec.edu.espe.asignaciones.entities.AuditAction;
import ec.edu.espe.asignaciones.events.AssignmentChangedEvent;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private static final String ENTIDAD = "ASIGNACION";

    private final AssignmentAuditEventRepository auditRepository;
    private final AuditPublisher auditPublisher;

    @EventListener
    public void onAssignmentChanged(AssignmentChangedEvent event) {
        AssignmentAuditEvent guardado = auditRepository.save(AssignmentAuditEvent.builder()
                .userId(event.assignmentId().getUserId())
                .vehicleId(event.assignmentId().getVehicleId())
                .action(event.action())
                .timestamp(OffsetDateTime.now(ZoneId.of("America/Guayaquil")))
                .oldPayload(event.oldPayload())
                .newPayload(event.newPayload())
                .build());

        auditPublisher.publicar(traducirAccion(event.action()), ENTIDAD, guardado);
    }

    private String traducirAccion(AuditAction action) {
        return switch (action) {
            case CREACION -> "CREATE";
            case MODIFICACION -> "UPDATE";
            case ELIMINACION -> "DELETE";
        };
    }
}
