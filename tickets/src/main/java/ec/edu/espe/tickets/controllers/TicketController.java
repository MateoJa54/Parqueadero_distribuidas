package ec.edu.espe.tickets.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @AuthenticationPrincipal String idEmpleado) {
        TicketResponse ticket = ticketService.registrarIngreso(request, UUID.fromString(idEmpleado));
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /** Cobra la estadia y cierra el ticket. */
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<TicketResponse> pagar(
            @PathVariable UUID id,
            @AuthenticationPrincipal String idEmpleado) {
        return ResponseEntity.ok(ticketService.pagar(id, UUID.fromString(idEmpleado)));
    }

    /** Anula un ticket activo por error humano. */
    @PatchMapping("/{id}/anular")
    public ResponseEntity<TicketResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody AnularTicketRequest request,
            @AuthenticationPrincipal String idEmpleado) {
        return ResponseEntity.ok(ticketService.anular(id, request, UUID.fromString(idEmpleado)));
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
    public ResponseEntity<Page<TicketResponse>> listar(
            @RequestParam(required = false) EstadoTicket estado,
            @PageableDefault(size = 20, sort = "fechaHoraIngreso", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.listar(estado, pageable));
    }

    /**
     * Tickets del usuario autenticado (propietario). El {@code CLIENTE} solo
     * puede consultar los suyos; el id se toma del token, no de un parametro.
     */
    @GetMapping("/mios")
    @PreAuthorize("hasAnyRole('CLIENTE','RECAUDADOR','ADMIN','ROOT')")
    public ResponseEntity<Page<TicketResponse>> misTickets(
            @AuthenticationPrincipal String idUsuario,
            @RequestParam(required = false) EstadoTicket estado,
            @PageableDefault(size = 20, sort = "fechaHoraIngreso", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ticketService.listarPorUsuario(UUID.fromString(idUsuario), estado, pageable));
    }

    @GetMapping("/activo/espacio/{idEspacio}")
    public ResponseEntity<TicketResponse> obtenerActivoPorEspacio(@PathVariable UUID idEspacio) {
        return ResponseEntity.ok(ticketService.obtenerActivoPorEspacio(idEspacio));
    }

    /**
     * Vehiculos que pueden ingresar: activos y sin un ticket ACTIVO (los que ya
     * estan dentro del parqueadero no aparecen). Alimenta el selector al crear tickets.
     */
    @GetMapping("/vehiculos-disponibles")
    public ResponseEntity<java.util.List<ec.edu.espe.tickets.dtos.VehiculoClientResponse>> vehiculosDisponibles() {
        return ResponseEntity.ok(ticketService.listarVehiculosSinTicketActivo());
    }
}
