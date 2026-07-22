package ec.edu.espe.asignaciones.services;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.asignaciones.dtos.AssignmentResponse;
import ec.edu.espe.asignaciones.dtos.AuditEventResponse;
import ec.edu.espe.asignaciones.dtos.CreateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.FleetVehicleResponse;
import ec.edu.espe.asignaciones.dtos.UpdateAssignmentRequest;
import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.entities.AssignmentId;
import ec.edu.espe.asignaciones.entities.AssignmentStatus;
import ec.edu.espe.asignaciones.entities.AuditAction;
import ec.edu.espe.asignaciones.entities.VehicleAssignment;
import ec.edu.espe.asignaciones.events.AssignmentChangedEvent;
import ec.edu.espe.asignaciones.repositories.AssignmentAuditEventRepository;
import ec.edu.espe.asignaciones.repositories.VehicleAssignmentRepository;
import ec.edu.espe.asignaciones.utils.RecursoNoEncontradoException;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final VehicleAssignmentRepository assignmentRepository;
    private final AssignmentAuditEventRepository auditRepository;
    private final ExternalCatalogService externalCatalogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public AssignmentResponse crearAsignacion(CreateAssignmentRequest request, String authorization) {
        UUID userId = request.getUserId();
        UUID vehicleId = request.getVehicleId();
        externalCatalogService.validarUsuarioActivo(userId, authorization);
        externalCatalogService.validarVehiculoActivo(vehicleId, authorization);
        UserRoleAssignmentResponse authorizationRole = externalCatalogService
                .validarRolAutorizadoParaAsignacion(userId, authorization);

        assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)
                .ifPresent(existing -> {
                    throw new ReglaNegocioException("El vehiculo ya tiene un propietario activo");
                });

        AssignmentId id = new AssignmentId(userId, vehicleId);
        VehicleAssignment assignment = assignmentRepository.findById(id)
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new ReglaNegocioException("La asignacion ya existe y esta activa");
                    }
                    existing.setActive(true);
                    existing.setStatus(AssignmentStatus.ACTIVA);
                    existing.setAuthorizationRoleId(authorizationRole.getIdRole());
                    existing.setAuthorizationRoleName(authorizationRole.getRol());
                    existing.setAssignmentType(request.getAssignmentType());
                    existing.setVehicleAlias(request.getVehicleAlias());
                    existing.setObservation(request.getObservation());
                    existing.setEntryAuthorized(true);
                    return existing;
                })
                .orElseGet(() -> VehicleAssignment.builder()
                        .id(id)
                        .active(true)
                        .status(AssignmentStatus.ACTIVA)
                        .assignmentType(request.getAssignmentType())
                        .authorizationRoleId(authorizationRole.getIdRole())
                        .authorizationRoleName(authorizationRole.getRol())
                        .vehicleAlias(request.getVehicleAlias())
                        .entryAuthorized(true)
                        .observation(request.getObservation())
                        .build());

        VehicleAssignment saved = assignmentRepository.save(assignment);
        eventPublisher.publishEvent(new AssignmentChangedEvent(id, AuditAction.CREACION, null, toJson(saved)));
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse modificarAsignacion(UUID userId, UUID vehicleId, UpdateAssignmentRequest request,
            String authorization) {
        AssignmentId id = new AssignmentId(userId, vehicleId);
        VehicleAssignment assignment = obtenerAsignacionActiva(id);
        String oldPayload = toJson(assignment);

        if (request.getStatus() != null) {
            aplicarCambioEstado(assignment, request, userId, authorization);
        }
        if (request.getAssignmentType() != null) {
            assignment.setAssignmentType(request.getAssignmentType());
        }
        if (request.getValidUntil() != null) {
            if (request.getValidUntil().isBefore(assignment.getValidFrom())) {
                throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio");
            }
            assignment.setValidUntil(request.getValidUntil());
        }
        if (request.getVehicleAlias() != null) {
            assignment.setVehicleAlias(request.getVehicleAlias());
        }
        if (request.getEntryAuthorized() != null) {
            if (Boolean.TRUE.equals(request.getEntryAuthorized()) && assignment.getStatus() != AssignmentStatus.ACTIVA) {
                throw new ReglaNegocioException("Solo una asignacion ACTIVA puede autorizar ingreso");
            }
            assignment.setEntryAuthorized(request.getEntryAuthorized());
        }
        if (request.getObservation() != null) {
            assignment.setObservation(request.getObservation());
        }
        if (request.getChangeReason() != null) {
            assignment.setChangeReason(request.getChangeReason());
        }

        VehicleAssignment saved = assignmentRepository.save(assignment);
        eventPublisher.publishEvent(new AssignmentChangedEvent(id, AuditAction.MODIFICACION, oldPayload, toJson(saved)));
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse desactivarAsignacion(UUID userId, UUID vehicleId) {
        AssignmentId id = new AssignmentId(userId, vehicleId);
        VehicleAssignment assignment = obtenerAsignacionActiva(id);
        String oldPayload = toJson(assignment);
        assignment.setActive(false);
        assignment.setStatus(AssignmentStatus.FINALIZADA);
        assignment.setEntryAuthorized(false);

        VehicleAssignment saved = assignmentRepository.save(assignment);
        eventPublisher.publishEvent(new AssignmentChangedEvent(id, AuditAction.ELIMINACION, oldPayload, toJson(saved)));
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse reactivarAsignacion(UUID userId, UUID vehicleId, String authorization) {
        externalCatalogService.validarUsuarioActivo(userId, authorization);
        externalCatalogService.validarVehiculoActivo(vehicleId, authorization);

        assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)
                .ifPresent(existing -> {
                    throw new ReglaNegocioException("El vehiculo ya tiene un propietario activo");
                });

        AssignmentId id = new AssignmentId(userId, vehicleId);
        VehicleAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignacion no encontrada"));
        if (assignment.isActive()) {
            throw new ReglaNegocioException("La asignacion ya esta activa");
        }
        UserRoleAssignmentResponse authorizationRole = externalCatalogService
                .validarRolAutorizadoParaAsignacion(userId, authorization);

        String oldPayload = toJson(assignment);
        assignment.setActive(true);
        assignment.setStatus(AssignmentStatus.ACTIVA);
        assignment.setEntryAuthorized(true);
        assignment.setAuthorizationRoleId(authorizationRole.getIdRole());
        assignment.setAuthorizationRoleName(authorizationRole.getRol());
        VehicleAssignment saved = assignmentRepository.save(assignment);
        eventPublisher.publishEvent(new AssignmentChangedEvent(id, AuditAction.MODIFICACION, oldPayload, toJson(saved)));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FleetVehicleResponse> consultarFlota(UUID userId, String authorization) {
        externalCatalogService.validarUsuarioActivo(userId, authorization);
        return assignmentRepository.findByIdUserIdAndActiveTrue(userId).stream()
                .map(assignment -> {
                    VehiculoClientResponse vehiculo = externalCatalogService
                            .obtenerVehiculo(assignment.getId().getVehicleId(), authorization);
                    // El cliente solo debe ver vehiculos activos: los inactivos (o
                    // inexistentes) se omiten sin romper la consulta de la flota.
                    if (vehiculo == null || !vehiculo.isActivo()) {
                        return null;
                    }
                    return FleetVehicleResponse.builder()
                            .userId(userId)
                            .vehicleId(assignment.getId().getVehicleId())
                            .placa(vehiculo.getPlaca())
                            .marca(vehiculo.getMarca())
                            .modelo(vehiculo.getModelo())
                            .color(vehiculo.getColor())
                            .anio(vehiculo.getAnio())
                            .tipo(vehiculo.getTipo())
                            .clasificacion(vehiculo.getClasificacion())
                            .activo(vehiculo.isActivo())
                            .status(assignment.getStatus())
                            .assignmentType(assignment.getAssignmentType())
                            .authorizationRoleName(assignment.getAuthorizationRoleName())
                            .validFrom(assignment.getValidFrom())
                            .validUntil(assignment.getValidUntil())
                            .vehicleAlias(assignment.getVehicleAlias())
                            .entryAuthorized(assignment.isEntryAuthorized())
                            .assignedAt(assignment.getAssignedAt())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse consultarAsignacionActivaPorVehiculo(UUID vehicleId) {
        VehicleAssignment assignment = assignmentRepository.findByIdVehicleIdAndActiveTrue(vehicleId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una asignacion activa para el vehiculo: " + vehicleId));
        return toResponse(assignment);
    }

    /**
     * Lista TODAS las asignaciones (para el panel de administracion). Si
     * {@code soloActivas} es {@code true} filtra por asignaciones activas.
     * Ordenadas por fecha de asignacion descendente (mas recientes primero).
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listarAsignaciones(boolean soloActivas) {
        return assignmentRepository.findAll().stream()
                .filter(a -> !soloActivas || a.isActive())
                .sorted(java.util.Comparator.comparing(
                        VehicleAssignment::getAssignedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> consultarTrazabilidad(UUID userId, UUID vehicleId) {
        return auditRepository.findByUserIdAndVehicleIdOrderByTimestampDesc(userId, vehicleId).stream()
                .map(event -> AuditEventResponse.builder()
                        .id(event.getId())
                        .userId(event.getUserId())
                        .vehicleId(event.getVehicleId())
                        .action(event.getAction())
                        .timestamp(event.getTimestamp())
                        .oldPayload(event.getOldPayload())
                        .newPayload(event.getNewPayload())
                        .build())
                .toList();
    }

    private void aplicarCambioEstado(VehicleAssignment assignment, UpdateAssignmentRequest request,
            UUID userId, String authorization) {
        AssignmentStatus previousStatus = assignment.getStatus();
        if (request.getStatus() == AssignmentStatus.ACTIVA && previousStatus != AssignmentStatus.ACTIVA) {
            externalCatalogService.validarUsuarioActivo(userId, authorization);
            UserRoleAssignmentResponse authorizationRole = externalCatalogService
                    .validarRolAutorizadoParaAsignacion(userId, authorization);
            assignment.setAuthorizationRoleId(authorizationRole.getIdRole());
            assignment.setAuthorizationRoleName(authorizationRole.getRol());
        }
        assignment.setStatus(request.getStatus());
        if (request.getStatus() == AssignmentStatus.FINALIZADA) {
            assignment.setActive(false);
        }
        if (request.getStatus() != AssignmentStatus.ACTIVA) {
            assignment.setEntryAuthorized(false);
        } else if (request.getEntryAuthorized() == null) {
            assignment.setEntryAuthorized(true);
        }
    }

    private VehicleAssignment obtenerAsignacionActiva(AssignmentId id) {
        VehicleAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignacion no encontrada"));
        if (!assignment.isActive()) {
            throw new ReglaNegocioException("La asignacion ya esta inactiva");
        }
        return assignment;
    }

    private AssignmentResponse toResponse(VehicleAssignment assignment) {
        return AssignmentResponse.builder()
                .userId(assignment.getId().getUserId())
                .vehicleId(assignment.getId().getVehicleId())
                .active(assignment.isActive())
                .status(assignment.getStatus())
                .assignmentType(assignment.getAssignmentType())
                .authorizationRoleId(assignment.getAuthorizationRoleId())
                .authorizationRoleName(assignment.getAuthorizationRoleName())
                .validFrom(assignment.getValidFrom())
                .validUntil(assignment.getValidUntil())
                .vehicleAlias(assignment.getVehicleAlias())
                .entryAuthorized(assignment.isEntryAuthorized())
                .observation(assignment.getObservation())
                .changeReason(assignment.getChangeReason())
                .assignedAt(assignment.getAssignedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private String toJson(VehicleAssignment assignment) {
        try {
            return objectMapper.writeValueAsString(toResponse(assignment));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la asignacion", ex);
        }
    }
}
