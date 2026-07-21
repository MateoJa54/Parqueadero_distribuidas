package ec.edu.espe.asignaciones.entities;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentAuditEventTest {

    @Test
    void builderAndGetters() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime ts = OffsetDateTime.now();

        AssignmentAuditEvent event = AssignmentAuditEvent.builder()
                .id(id)
                .userId(userId)
                .vehicleId(vehicleId)
                .action(AuditAction.CREACION)
                .timestamp(ts)
                .oldPayload("{}")
                .newPayload("{\"x\":1}")
                .build();

        assertEquals(id, event.getId());
        assertEquals(userId, event.getUserId());
        assertEquals(vehicleId, event.getVehicleId());
        assertEquals(AuditAction.CREACION, event.getAction());
        assertEquals(ts, event.getTimestamp());
        assertEquals("{}", event.getOldPayload());
        assertEquals("{\"x\":1}", event.getNewPayload());
    }

    @Test
    void noArgsConstructor() {
        AssignmentAuditEvent event = new AssignmentAuditEvent();
        assertNotNull(event);
    }
}
