package ec.edu.espe.asignaciones.services;

import java.time.OffsetDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ec.edu.espe.asignaciones.entities.AssignmentAuditEvent;
import ec.edu.espe.asignaciones.events.AssignmentChangedEvent;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AssignmentAuditEventRepository auditRepository;

    @EventListener
    public void onAssignmentChanged(AssignmentChangedEvent event) {
        auditRepository.save(AssignmentAuditEvent.builder()
                .userId(event.assignmentId().getUserId())
                .vehicleId(event.assignmentId().getVehicleId())
                .action(event.action())
                .timestamp(OffsetDateTime.now())
                .oldPayload(event.oldPayload())
                .newPayload(event.newPayload())
                .build());
    }
}
