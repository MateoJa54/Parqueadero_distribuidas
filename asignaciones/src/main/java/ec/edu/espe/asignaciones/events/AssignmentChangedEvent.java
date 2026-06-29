package ec.edu.espe.asignaciones.events;

import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AuditAction;

public record AssignmentChangedEvent(
        AssignmentId assignmentId,
        AuditAction action,
        String oldPayload,
        String newPayload) {
}
