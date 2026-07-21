package ec.edu.espe.asignaciones.services;

import ec.edu.espe.asignaciones.audit.AuditPublisher;
import ec.edu.espe.asignaciones.entities.AssignmentAuditEvent;
import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AuditAction;
import ec.edu.espe.asignaciones.events.AssignmentChangedEvent;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditEventListenerTest {

    private AssignmentAuditEventRepository auditRepository;
    private AuditPublisher auditPublisher;
    private AuditEventListener listener;

    @BeforeEach
    void setUp() {
        auditRepository = mock(AssignmentAuditEventRepository.class);
        auditPublisher = mock(AuditPublisher.class);
        listener = new AuditEventListener(auditRepository, auditPublisher);
    }

    private AssignmentAuditEvent savedEvent(UUID userId, UUID vehicleId, AuditAction action) {
        return AssignmentAuditEvent.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .vehicleId(vehicleId)
                .action(action)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("onAssignmentChanged CREACION llama auditPublisher con CREATE")
    void creacion() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId assignmentId = new AssignmentId(userId, vehicleId);
        AssignmentChangedEvent event = new AssignmentChangedEvent(assignmentId, AuditAction.CREACION, null, "{}");
        AssignmentAuditEvent saved = savedEvent(userId, vehicleId, AuditAction.CREACION);

        when(auditRepository.save(any())).thenReturn(saved);

        listener.onAssignmentChanged(event);

        verify(auditRepository).save(any());
        verify(auditPublisher).publicar(eq("CREATE"), eq("ASIGNACION"), any());
    }

    @Test
    @DisplayName("onAssignmentChanged MODIFICACION llama auditPublisher con UPDATE")
    void modificacion() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId assignmentId = new AssignmentId(userId, vehicleId);
        AssignmentChangedEvent event = new AssignmentChangedEvent(assignmentId, AuditAction.MODIFICACION, "{}", "{}");
        AssignmentAuditEvent saved = savedEvent(userId, vehicleId, AuditAction.MODIFICACION);

        when(auditRepository.save(any())).thenReturn(saved);

        listener.onAssignmentChanged(event);

        verify(auditPublisher).publicar(eq("UPDATE"), eq("ASIGNACION"), any());
    }

    @Test
    @DisplayName("onAssignmentChanged ELIMINACION llama auditPublisher con DELETE")
    void eliminacion() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId assignmentId = new AssignmentId(userId, vehicleId);
        AssignmentChangedEvent event = new AssignmentChangedEvent(assignmentId, AuditAction.ELIMINACION, "{}", null);
        AssignmentAuditEvent saved = savedEvent(userId, vehicleId, AuditAction.ELIMINACION);

        when(auditRepository.save(any())).thenReturn(saved);

        listener.onAssignmentChanged(event);

        verify(auditPublisher).publicar(eq("DELETE"), eq("ASIGNACION"), any());
    }
}
