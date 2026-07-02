package ec.edu.espe.asignaciones.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.asignaciones.dtos.AssignmentResponse;
import ec.edu.espe.asignaciones.dtos.AuditEventResponse;
import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
import ec.edu.espe.asignaciones.services.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/asignaciones-vehiculos")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<AssignmentResponse> crearAsignacion(
            @Valid @RequestBody CreateAssignmentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return new ResponseEntity<>(
                assignmentService.crearAsignacion(request, authorization),
                HttpStatus.CREATED);
    }

    @PatchMapping("/{userId}/{vehicleId}")
    public ResponseEntity<AssignmentResponse> modificarAsignacion(
            @PathVariable UUID userId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateAssignmentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(assignmentService.modificarAsignacion(userId, vehicleId, request, authorization));
    }

    @PatchMapping("/{userId}/{vehicleId}/desactivar")
    public ResponseEntity<AssignmentResponse> desactivarAsignacion(
            @PathVariable UUID userId,
            @PathVariable UUID vehicleId) {
        return ResponseEntity.ok(assignmentService.desactivarAsignacion(userId, vehicleId));
    }

    @PatchMapping("/{userId}/{vehicleId}/activar")
    public ResponseEntity<AssignmentResponse> reactivarAsignacion(
            @PathVariable UUID userId,
            @PathVariable UUID vehicleId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(assignmentService.reactivarAsignacion(userId, vehicleId, authorization));
    }

    @GetMapping("/vehiculo/{vehicleId}")
    public ResponseEntity<AssignmentResponse> consultarAsignacionActivaPorVehiculo(
            @PathVariable UUID vehicleId) {
        return ResponseEntity.ok(assignmentService.consultarAsignacionActivaPorVehiculo(vehicleId));
    }

    @GetMapping("/{userId}/{vehicleId}/trazabilidad")
    public ResponseEntity<List<AuditEventResponse>> consultarTrazabilidad(
            @PathVariable UUID userId,
            @PathVariable UUID vehicleId) {
        return ResponseEntity.ok(assignmentService.consultarTrazabilidad(userId, vehicleId));
    }
}
