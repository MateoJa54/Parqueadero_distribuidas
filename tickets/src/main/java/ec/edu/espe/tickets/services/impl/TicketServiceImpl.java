package ec.edu.espe.tickets.services.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.tickets.dtos.AnularTicketRequest;
import ec.edu.espe.tickets.dtos.AsignacionActivaResponse;
import ec.edu.espe.tickets.dtos.EspacioClientResponse;
import ec.edu.espe.tickets.dtos.RegistrarIngresoRequest;
import ec.edu.espe.tickets.dtos.TicketResponse;
import ec.edu.espe.tickets.dtos.VehiculoClientResponse;
import ec.edu.espe.tickets.entities.EstadoTicket;
import ec.edu.espe.tickets.entities.Ticket;
import ec.edu.espe.tickets.repositories.TicketRepository;
import ec.edu.espe.tickets.services.CalculadoraTarifa;
import ec.edu.espe.tickets.services.CatalogoExternoService;
import ec.edu.espe.tickets.services.GeneradorCodigoTicket;
import ec.edu.espe.tickets.services.TicketService;
import ec.edu.espe.tickets.utils.CompatibilidadTipos;
import ec.edu.espe.tickets.utils.RecursoNoEncontradoException;
import ec.edu.espe.tickets.utils.ReglaNegocioException;
import ec.edu.espe.tickets.utils.TicketMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final String ESTADO_ASIGNACION_ACTIVA = "ACTIVA";
    private static final String ESTADO_ESPACIO_DISPONIBLE = "DISPONIBLE";
    private static final String ESTADO_ESPACIO_OCUPADO = "OCUPADO";

    private final TicketRepository ticketRepository;
    private final CatalogoExternoService catalogo;
    private final CalculadoraTarifa calculadoraTarifa;
    private final GeneradorCodigoTicket generadorCodigo;

    @Override
    @Transactional
    public TicketResponse registrarIngreso(RegistrarIngresoRequest request, UUID idEmpleado) {
        String placa = request.getPlaca().trim().toUpperCase();

        VehiculoClientResponse vehiculo = catalogo.obtenerVehiculoPorPlaca(placa);
        if (!vehiculo.isActivo()) {
            throw new ReglaNegocioException("El vehiculo con placa " + placa + " esta inactivo");
        }

        AsignacionActivaResponse asignacion =
                catalogo.obtenerAsignacionActivaPorVehiculo(vehiculo.getId());
        validarAsignacion(asignacion, placa);

        ticketRepository.findByIdVehiculoAndEstadoTicket(vehiculo.getId(), EstadoTicket.ACTIVO)
                .ifPresent(t -> {
                    throw new ReglaNegocioException(
                            "El vehiculo ya tiene un ticket activo: " + t.getCodigo());
                });

        EspacioClientResponse espacio = catalogo.obtenerEspacio(request.getIdEspacio());
        validarEspacioDisponible(espacio);
        validarCompatibilidad(vehiculo.getTipo(), espacio.getTipo());

        ticketRepository.findByIdEspacioAndEstadoTicket(espacio.getId(), EstadoTicket.ACTIVO)
                .ifPresent(t -> {
                    throw new ReglaNegocioException(
                            "El espacio ya tiene un ticket activo: " + t.getCodigo());
                });

        Ticket ticket = Ticket.builder()
                .codigo(generadorCodigo.generar())
                .idEspacio(espacio.getId())
                .codigoEspacio(espacio.getCodigo())
                .tipoEspacio(CompatibilidadTipos.normalizar(espacio.getTipo()))
                .idUsuario(asignacion.getUserId())
                .idVehiculo(vehiculo.getId())
                .placa(placa)
                .tipoVehiculo(vehiculo.getTipo())
                .fechaHoraIngreso(OffsetDateTime.now())
                .estadoTicket(EstadoTicket.ACTIVO)
                .idEmpleado(idEmpleado)
                .valorRecaudado(BigDecimal.ZERO)
                .build();

        Ticket guardado = ticketRepository.save(ticket);

        // Ultimo paso: ocupar el espacio. Si el PATCH remoto falla, la excepcion
        // propaga y @Transactional revierte la insercion del ticket.
        catalogo.cambiarEstadoEspacio(espacio.getId(), ESTADO_ESPACIO_OCUPADO);

        return TicketMapper.aResponse(guardado);
    }

    @Override
    @Transactional
    public TicketResponse pagar(UUID idTicket, UUID idEmpleado) {
        Ticket ticket = buscar(idTicket);
        if (ticket.getEstadoTicket() != EstadoTicket.ACTIVO) {
            throw new ReglaNegocioException(
                    "Solo se pueden pagar tickets activos (estado actual: "
                            + ticket.getEstadoTicket() + ")");
        }

        OffsetDateTime salida = OffsetDateTime.now();
        BigDecimal valor = calculadoraTarifa.calcular(
                ticket.getTipoVehiculo(), ticket.getTipoEspacio(),
                ticket.getFechaHoraIngreso(), salida);

        ticket.setFechaHoraSalida(salida);
        ticket.setValorRecaudado(valor);
        ticket.setEstadoTicket(EstadoTicket.PAGADO);
        ticket.setIdEmpleado(idEmpleado);
        Ticket guardado = ticketRepository.save(ticket);

        catalogo.cambiarEstadoEspacio(ticket.getIdEspacio(), ESTADO_ESPACIO_DISPONIBLE);

        return TicketMapper.aResponse(guardado);
    }

    @Override
    @Transactional
    public TicketResponse anular(UUID idTicket, AnularTicketRequest request, UUID idEmpleado) {
        Ticket ticket = buscar(idTicket);
        if (ticket.getEstadoTicket() != EstadoTicket.ACTIVO) {
            throw new ReglaNegocioException(
                    "Solo se pueden anular tickets activos; un ticket "
                            + ticket.getEstadoTicket() + " no se puede anular");
        }

        ticket.setEstadoTicket(EstadoTicket.ANULADO);
        ticket.setValorRecaudado(BigDecimal.ZERO);
        ticket.setFechaHoraSalida(OffsetDateTime.now());
        ticket.setMotivoAnulacion(request.getMotivo().trim());
        ticket.setIdEmpleado(idEmpleado);
        Ticket guardado = ticketRepository.save(ticket);

        catalogo.cambiarEstadoEspacio(ticket.getIdEspacio(), ESTADO_ESPACIO_DISPONIBLE);

        return TicketMapper.aResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtenerPorId(UUID idTicket) {
        return TicketMapper.aResponse(buscar(idTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtenerPorCodigo(String codigo) {
        Ticket ticket = ticketRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un ticket con codigo: " + codigo));
        return TicketMapper.aResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listar(EstadoTicket estado) {
        List<Ticket> tickets = (estado == null)
                ? ticketRepository.findAllByOrderByFechaHoraIngresoDesc()
                : ticketRepository.findByEstadoTicketOrderByFechaHoraIngresoDesc(estado);
        return tickets.stream().map(TicketMapper::aResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtenerActivoPorEspacio(UUID idEspacio) {
        Ticket ticket = ticketRepository
                .findByIdEspacioAndEstadoTicket(idEspacio, EstadoTicket.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El espacio no tiene un ticket activo: " + idEspacio));
        return TicketMapper.aResponse(ticket);
    }

    // ------------------------------------------------------------------
    // Validaciones privadas
    // ------------------------------------------------------------------

    private Ticket buscar(UUID idTicket) {
        return ticketRepository.findById(idTicket)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un ticket con id: " + idTicket));
    }

    private void validarAsignacion(AsignacionActivaResponse asignacion, String placa) {
        if (!asignacion.isActive()) {
            throw new ReglaNegocioException(
                    "La asignacion del vehiculo con placa " + placa + " no esta vigente");
        }
        if (!ESTADO_ASIGNACION_ACTIVA.equalsIgnoreCase(asignacion.getStatus())) {
            throw new ReglaNegocioException(
                    "La asignacion del vehiculo no esta ACTIVA (estado actual: "
                            + asignacion.getStatus() + ")");
        }
        if (!asignacion.isEntryAuthorized()) {
            throw new ReglaNegocioException(
                    "El vehiculo con placa " + placa + " no tiene autorizacion de ingreso");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (asignacion.getValidFrom() != null && ahora.isBefore(asignacion.getValidFrom())) {
            throw new ReglaNegocioException(
                    "La asignacion aun no es vigente (valida desde " + asignacion.getValidFrom() + ")");
        }
        if (asignacion.getValidUntil() != null && !ahora.isBefore(asignacion.getValidUntil())) {
            throw new ReglaNegocioException(
                    "La asignacion expiro (valida hasta " + asignacion.getValidUntil() + ")");
        }
    }

    private void validarEspacioDisponible(EspacioClientResponse espacio) {
        if (!espacio.isActivo()) {
            throw new ReglaNegocioException("El espacio " + espacio.getCodigo() + " esta inactivo");
        }
        if (!ESTADO_ESPACIO_DISPONIBLE.equalsIgnoreCase(espacio.getEstado())) {
            throw new ReglaNegocioException(
                    "El espacio " + espacio.getCodigo() + " no esta disponible (estado actual: "
                            + espacio.getEstado() + ")");
        }
    }

    private void validarCompatibilidad(String tipoVehiculo, String tipoEspacio) {
        if (!CompatibilidadTipos.sonCompatibles(tipoVehiculo, tipoEspacio)) {
            String esperado = CompatibilidadTipos.espacioRequeridoPara(tipoVehiculo);
            throw new ReglaNegocioException(
                    "El vehiculo tipo " + tipoVehiculo + " no es compatible con un espacio "
                            + tipoEspacio + (esperado != null
                                    ? "; requiere un espacio " + esperado
                                    : "; tipo de vehiculo no reconocido"));
        }
    }
}
