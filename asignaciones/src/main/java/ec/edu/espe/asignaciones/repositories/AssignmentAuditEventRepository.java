package ec.edu.espe.asignaciones.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.asignaciones.entities.AssignmentAuditEvent;

public interface AssignmentAuditEventRepository extends JpaRepository<AssignmentAuditEvent, UUID> {

    List<AssignmentAuditEvent> findByUserIdAndVehicleIdOrderByTimestampDesc(UUID userId, UUID vehicleId);
}
