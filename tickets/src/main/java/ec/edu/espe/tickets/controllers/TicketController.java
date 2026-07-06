package ec.edu.espe.tickets.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.tickets.dtos.AnularTicketRequest;
import ec.edu.espe.tickets.dtos.RegistrarIngresoRequest;
import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.security.RolesTickets;
import ec.edu.espe.tickets.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API de tickets de parqueadero. Todas las operaciones exigen un JWT valido con
 * rol RECAUDADOR, ADMIN o ROOT; el empleado que opera se toma del token (sub).
 */
@RestController
@RequestMapping("/api/v1/tickets")
@PreAuthorize(RolesTickets.PUEDE_OPERAR)
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** Registra el ingreso de un vehiculo por placa. */
    @PostMapping
    public ResponseEntity<TicketResponse> registrarIngreso(
            @Valid @RequestBody RegistrarIngresoRequest request,
            @AuthenticationPrincipal String idEmpleado,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        TicketResponse ticket = ticketService.registrarIngreso(request, UUID.fromString(idEmpleado), authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /** Cobra la estadia y cierra el ticket. */
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<TicketResponse> pagar(
            @PathVariable UUID id,
            @AuthenticationPrincipal String idEmpleado,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(ticketService.pagar(id, UUID.fromString(idEmpleado), authorization));
    }

    /** Anula un ticket activo por error humano. */
    @PatchMapping("/{id}/anular")
    public ResponseEntity<TicketResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody AnularTicketRequest request,
            @AuthenticationPrincipal String idEmpleado,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(ticketService.anular(id, request, UUID.fromString(idEmpleado), authorization));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.obtenerPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<TicketResponse> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(ticketService.obtenerPorCodigo(codigo));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listar(
            @RequestParam(required = false) EstadoTicket estado) {
        return ResponseEntity.ok(ticketService.listar(estado));
    }

    @GetMapping("/activo/espacio/{idEspacio}")
    public ResponseEntity<TicketResponse> obtenerActivoPorEspacio(@PathVariable UUID idEspacio) {
        return ResponseEntity.ok(ticketService.obtenerActivoPorEspacio(idEspacio));
    }
}
