package ec.edu.espe.tickets.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByCodigo(String codigo);

    Optional<Ticket> findByCodigo(String codigo);

    /** Un vehiculo no puede tener dos tickets ACTIVOS a la vez. */
    Optional<Ticket> findByIdVehiculoAndEstadoTicket(UUID idVehiculo, EstadoTicket estadoTicket);

    /** Un espacio no puede estar ocupado por dos tickets ACTIVOS a la vez. */
    Optional<Ticket> findByIdEspacioAndEstadoTicket(UUID idEspacio, EstadoTicket estadoTicket);

    List<Ticket> findByEstadoTicketOrderByFechaHoraIngresoDesc(EstadoTicket estadoTicket);

    List<Ticket> findAllByOrderByFechaHoraIngresoDesc();
}
