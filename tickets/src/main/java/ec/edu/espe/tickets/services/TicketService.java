package ec.edu.espe.tickets.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.tickets.dtos.AnularTicketRequest;
import ec.edu.espe.tickets.dtos.RegistrarIngresoRequest;
import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;

/** Casos de uso del ticket de parqueadero. */
public interface TicketService {

    /** Registra el ingreso de un vehiculo (por placa) a un espacio. */
    TicketResponse registrarIngreso(RegistrarIngresoRequest request, UUID idEmpleado, String authorization);

    /** Cierra el ticket cobrando la estadia y libera el espacio. */
    TicketResponse pagar(UUID idTicket, UUID idEmpleado, String authorization);

    /** Anula un ticket activo por error humano y libera el espacio. */
    TicketResponse anular(UUID idTicket, AnularTicketRequest request, UUID idEmpleado, String authorization);

    TicketResponse obtenerPorId(UUID idTicket);

    TicketResponse obtenerPorCodigo(String codigo);

    /** Lista los tickets; si {@code estado} es null devuelve todos. */
    List<TicketResponse> listar(EstadoTicket estado);

    /** Devuelve el ticket activo que ocupa un espacio. */
    TicketResponse obtenerActivoPorEspacio(UUID idEspacio);
}
