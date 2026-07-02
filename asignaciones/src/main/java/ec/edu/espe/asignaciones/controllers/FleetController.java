package ec.edu.espe.asignaciones.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.asignaciones.dtos.FleetVehicleResponse;
import ec.edu.espe.asignaciones.services.AssignmentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/propietarios")
@RequiredArgsConstructor
public class FleetController {

    private final AssignmentService assignmentService;

    @GetMapping("/{userId}/vehiculos")
    public ResponseEntity<List<FleetVehicleResponse>> consultarFlota(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(assignmentService.consultarFlota(userId, authorization));
    }
}
