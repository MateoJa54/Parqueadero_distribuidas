package ec.edu.espe.tickets.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByCodigo(String codigo);

    /**
     * Siguiente numero para el codigo de ticket, tomado de una secuencia de la BD.
     * Es atomico y thread-safe (a diferencia de count()+1), evitando codigos
     * duplicados ante concurrencia.
     */
    @Query(value = "SELECT nextval('ticket_codigo_seq')", nativeQuery = true)
    long siguienteNumeroCodigoTicket();

    Optional<Ticket> findByCodigo(String codigo);

    /** Un vehiculo no puede tener dos tickets ACTIVOS a la vez. */
    Optional<Ticket> findByIdVehiculoAndEstadoTicket(UUID idVehiculo, EstadoTicket estadoTicket);

    /** Un espacio no puede estar ocupado por dos tickets ACTIVOS a la vez. */
    Optional<Ticket> findByIdEspacioAndEstadoTicket(UUID idEspacio, EstadoTicket estadoTicket);

    Page<Ticket> findByEstadoTicketOrderByFechaHoraIngresoDesc(EstadoTicket estadoTicket, Pageable pageable);

    Page<Ticket> findAllByOrderByFechaHoraIngresoDesc(Pageable pageable);

    /** Tickets de un propietario (para que el cliente vea solo los suyos). */
    Page<Ticket> findByIdUsuarioOrderByFechaHoraIngresoDesc(UUID idUsuario, Pageable pageable);

    Page<Ticket> findByIdUsuarioAndEstadoTicketOrderByFechaHoraIngresoDesc(
            UUID idUsuario, EstadoTicket estadoTicket, Pageable pageable);

    /** Ids de vehiculos que actualmente tienen un ticket en el estado dado (p.ej. ACTIVO). */
    @Query("SELECT t.idVehiculo FROM Ticket t WHERE t.estadoTicket = :estado")
    List<UUID> idsVehiculoPorEstado(EstadoTicket estado);
}
