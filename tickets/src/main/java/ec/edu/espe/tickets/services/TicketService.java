package ec.edu.espe.tickets.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ec.edu.espe.tickets.dtos.AnularTicketRequest;
import ec.edu.espe.tickets.dtos.RegistrarIngresoRequest;
import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.dtos.VehiculoClientResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;

/** Casos de uso del ticket de parqueadero. */
public interface TicketService {

    /** Registra el ingreso de un vehiculo (por placa) a un espacio. */
    TicketResponse registrarIngreso(RegistrarIngresoRequest request, UUID idEmpleado);

    /** Cierra el ticket cobrando la estadia y libera el espacio. */
    TicketResponse pagar(UUID idTicket, UUID idEmpleado);

    /** Anula un ticket activo por error humano y libera el espacio. */
    TicketResponse anular(UUID idTicket, AnularTicketRequest request, UUID idEmpleado);

    TicketResponse obtenerPorId(UUID idTicket);

    TicketResponse obtenerPorCodigo(String codigo);

    /** Lista paginada de tickets; si {@code estado} es null devuelve todos. */
    Page<TicketResponse> listar(EstadoTicket estado, Pageable pageable);

    /** Lista paginada de tickets de un propietario; si {@code estado} es null devuelve todos. */
    Page<TicketResponse> listarPorUsuario(UUID idUsuario, EstadoTicket estado, Pageable pageable);

    /** Devuelve el ticket activo que ocupa un espacio. */
    TicketResponse obtenerActivoPorEspacio(UUID idEspacio);

    /**
     * Vehiculos activos del catalogo que NO tienen un ticket ACTIVO (es decir,
     * no estan actualmente en el parqueadero) y por tanto pueden ingresar.
     */
    List<VehiculoClientResponse> listarVehiculosSinTicketActivo();
}
