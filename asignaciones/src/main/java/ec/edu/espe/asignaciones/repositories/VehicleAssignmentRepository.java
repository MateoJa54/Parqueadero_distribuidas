package ec.edu.espe.asignaciones.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.VehicleAssignment;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, AssignmentId> {

    List<VehicleAssignment> findByIdUserIdAndActiveTrue(UUID userId);

    Optional<VehicleAssignment> findByIdVehicleIdAndActiveTrue(UUID vehicleId);
}
