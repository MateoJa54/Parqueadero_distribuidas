package ec.edu.espe.asignaciones.events;

import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AuditAction;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentChangedEventTest {

    @Test
    void recordAccessors() {
        AssignmentId id = new AssignmentId(UUID.randomUUID(), UUID.randomUUID());
        AssignmentChangedEvent event = new AssignmentChangedEvent(id, AuditAction.CREACION, "old", "new");

        assertEquals(id, event.assignmentId());
        assertEquals(AuditAction.CREACION, event.action());
        assertEquals("old", event.oldPayload());
        assertEquals("new", event.newPayload());
    }

    @Test
    void nullPayloadsAllowed() {
        AssignmentId id = new AssignmentId(UUID.randomUUID(), UUID.randomUUID());
        AssignmentChangedEvent event = new AssignmentChangedEvent(id, AuditAction.ELIMINACION, null, null);

        assertNull(event.oldPayload());
        assertNull(event.newPayload());
    }
}
