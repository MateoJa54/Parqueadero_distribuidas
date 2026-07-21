package ec.edu.espe.asignaciones.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentIdTest {

    @Test
    void defaultConstructor() {
        AssignmentId id = new AssignmentId();
        assertNull(id.getUserId());
        assertNull(id.getVehicleId());
    }

    @Test
    void allArgsConstructor() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId id = new AssignmentId(userId, vehicleId);
        assertEquals(userId, id.getUserId());
        assertEquals(vehicleId, id.getVehicleId());
    }

    @Test
    void equalsAndHashCode() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId id1 = new AssignmentId(userId, vehicleId);
        AssignmentId id2 = new AssignmentId(userId, vehicleId);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void notEqualDifferentIds() {
        AssignmentId id1 = new AssignmentId(UUID.randomUUID(), UUID.randomUUID());
        AssignmentId id2 = new AssignmentId(UUID.randomUUID(), UUID.randomUUID());
        assertNotEquals(id1, id2);
    }

    @Test
    void toStringContainsIds() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId id = new AssignmentId(userId, vehicleId);
        String str = id.toString();
        assertTrue(str.contains(userId.toString()));
        assertTrue(str.contains(vehicleId.toString()));
    }

    @Test
    void setters() {
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        AssignmentId id = new AssignmentId();
        id.setUserId(userId);
        id.setVehicleId(vehicleId);
        assertEquals(userId, id.getUserId());
        assertEquals(vehicleId, id.getVehicleId());
    }
}
