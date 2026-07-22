package ec.edu.espe.zonas.services.Impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.zonas.audit.AuditPublisher;
import ec.edu.espe.zonas.dtos.DisponibilidadResponseDto;
import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.entidades.Zona;
import ec.edu.espe.zonas.repositorios.EspacioRepositorio;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;
import ec.edu.espe.zonas.services.EspacioServicio;
import ec.edu.espe.zonas.sse.SseService;
import ec.edu.espe.zonas.utils.RecursoNoEncontradoException;
import ec.edu.espe.zonas.utils.ReglaNegocioException;
import ec.edu.espe.zonas.utils.UtilMapers;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class EspacioServicioImpl implements EspacioServicio {

    private static final String ENTIDAD = "ESPACIO";
    private static final String ESPACIO_NOT_FOUND = "Espacio no encontrado con ID: ";
    private static final String ZONA_NOT_FOUND = "Zona no encontrada con ID: ";
    private static final String AUDIT_UPDATE = "UPDATE";

    // Nombres de los eventos que consume el dashboard de monitoreo (SSE).
    private static final String EVENTO_CREADO = "espacio-creado";
    private static final String EVENTO_ACTUALIZADO = "espacio-actualizado";

    private final EspacioRepositorio espacioRepositorio;
    private final ZonaRepositorio zonaRepositorio;
    private final UtilMapers maper;
    private final AuditPublisher auditPublisher;
    private final SseService sseService;

    @Override
    @Transactional(readOnly = true)
    public List<EspacioRespondeDto> obtenerEspacio() {
        return espacioRepositorio.findAll().stream()
                .map(maper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspacioRespondeDto obtenerEspacioPorId(UUID idEspacio) {
        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));
        return maper.toResponseDto(espacio);
    }

    @Override
    @Transactional
    public EspacioRespondeDto crearEspacio(EspacioRequestDto dto) {

        Zona objZona = zonaRepositorio.findById(dto.getIdZona())
                .orElseThrow(() -> new RecursoNoEncontradoException(ZONA_NOT_FOUND + dto.getIdZona()));

        if (!objZona.isActivo()) {
            throw new ReglaNegocioException("No se puede crear un espacio en una zona inactiva");
        }

        long espaciosActuales = espacioRepositorio.countByZona(objZona);
        if (espaciosActuales >= objZona.getCapacidad()) {
            throw new ReglaNegocioException(
                    "La zona alcanzó su capacidad máxima (" + objZona.getCapacidad() + ")");
        }

        // El código del espacio SIEMPRE se autogenera; no se acepta uno del cliente.
        String codigo = generarCodigoEspacio(dto);

        Espacio espacio = maper.toEntityEspacio(dto);
        espacio.setCodigo(codigo);
        espacio.setZona(objZona);
        espacio.setActivo(true);
        espacio.setEstado(EstadoEspacio.DISPONIBLE);

        Espacio espacioSaved = espacioRepositorio.save(espacio);
        auditPublisher.publicar("CREATE", ENTIDAD, espacioSaved);

        EspacioRespondeDto respuesta = maper.toResponseDto(espacioSaved);
        sseService.emitir(EVENTO_CREADO, respuesta);
        return respuesta;
    }

    @Override
    @Transactional
    public EspacioRespondeDto actualizarEspacio(UUID idEspacio, EspacioRequestDto dto) {
        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));

        // Solo se actualizan atributos descriptivos. El codigo es inmutable,
        // la zona no se reasigna (rompería la capacidad), y el estado/activo
        // se gestionan con sus endpoints dedicados.
        espacio.setDescripcion(dto.getDescripcion());
        espacio.setTipoEspacio(dto.getTipo());

        Espacio espacioActualizado = espacioRepositorio.save(espacio);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, espacioActualizado);

        EspacioRespondeDto respuesta = maper.toResponseDto(espacioActualizado);
        sseService.emitir(EVENTO_ACTUALIZADO, respuesta);
        return respuesta;
    }

    @Override
    @Transactional
    public EspacioRespondeDto cambiarEstado(UUID idEspacio, EstadoEspacio estado) {

        if (idEspacio == null) {
            throw new IllegalArgumentException("El id del espacio es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }

        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));

        if (!espacio.isActivo()) {
            throw new ReglaNegocioException("No se puede cambiar el estado de un espacio inactivo");
        }
        if (espacio.getEstado() == estado) {
            throw new ReglaNegocioException("El espacio ya se encuentra en estado: " + estado);
        }

        espacio.setEstado(estado);

        Espacio espacioActualizado = espacioRepositorio.save(espacio);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, espacioActualizado);

        // Este es el punto por el que pasa el ticket: al emitir/pagar/anular, el
        // microservicio de tickets llama al PATCH de estado y aqui se difunde.
        EspacioRespondeDto respuesta = maper.toResponseDto(espacioActualizado);
        sseService.emitir(EVENTO_ACTUALIZADO, respuesta);
        return respuesta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacioRespondeDto> obtenerEspacioPorEstado(EstadoEspacio estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        return espacioRepositorio.findByEstado(estado).stream()
                .map(maper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EspacioRespondeDto obtenerEspacioPorZonaEstado(UUID idZona, EstadoEspacio estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        Zona objZona = zonaRepositorio.findById(idZona)
                .orElseThrow(() -> new RecursoNoEncontradoException(ZONA_NOT_FOUND + idZona));

        return espacioRepositorio.findByZonaAndEstado(objZona, estado).stream()
                .findFirst()
                .map(maper::toResponseDto)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningun espacio en estado " + estado + " para la zona con ID: " + idZona));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacioRespondeDto> listarDisponibles(UUID idZona, TipoEspacio tipo) {
        // "Disponible" = estado DISPONIBLE. Los filtros zona/tipo son opcionales.
        List<Espacio> espacios;

        if (idZona != null) {
            Zona objZona = zonaRepositorio.findById(idZona)
                    .orElseThrow(() -> new RecursoNoEncontradoException(ZONA_NOT_FOUND + idZona));
            espacios = (tipo != null)
                    ? espacioRepositorio.findByZonaAndTipoEspacioAndEstado(objZona, tipo, EstadoEspacio.DISPONIBLE)
                    : espacioRepositorio.findByZonaAndEstado(objZona, EstadoEspacio.DISPONIBLE);
        } else if (tipo != null) {
            espacios = espacioRepositorio.findByTipoEspacioAndEstado(tipo, EstadoEspacio.DISPONIBLE);
        } else {
            espacios = espacioRepositorio.findByEstado(EstadoEspacio.DISPONIBLE);
        }

        // Solo se consideran disponibles los espacios activos.
        return espacios.stream()
                .filter(Espacio::isActivo)
                .map(maper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResponseDto verificarDisponibilidad(UUID idEspacio) {
        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));

        boolean disponible = espacio.isActivo() && espacio.getEstado() == EstadoEspacio.DISPONIBLE;

        return DisponibilidadResponseDto.builder()
                .idEspacio(espacio.getId())
                .codigo(espacio.getCodigo())
                .disponible(disponible)
                .activo(espacio.isActivo())
                .estado(espacio.getEstado())
                .build();
    }

    @Override
    @Transactional
    public void activarEspacio(UUID idEspacio) {
        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));
        if (espacio.isActivo()) {
            throw new ReglaNegocioException("El espacio ya está activo");
        }
        // Invariante: un espacio no puede estar activo si su zona está inactiva.
        if (!espacio.getZona().isActivo()) {
            throw new ReglaNegocioException("No se puede activar el espacio: su zona está inactiva");
        }
        espacio.setActivo(true);
        espacio.setEstado(EstadoEspacio.DISPONIBLE);
        espacioRepositorio.save(espacio);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, espacio);
        sseService.emitir(EVENTO_ACTUALIZADO, maper.toResponseDto(espacio));
    }

    @Override
    @Transactional
    public void desactivarEspacio(UUID idEspacio) {
        Espacio espacio = espacioRepositorio.findById(idEspacio)
                .orElseThrow(() -> new RecursoNoEncontradoException(ESPACIO_NOT_FOUND + idEspacio));
        if (!espacio.isActivo()) {
            throw new ReglaNegocioException("El espacio ya está inactivo");
        }
        if (espacio.getEstado() == EstadoEspacio.OCUPADO) {
            throw new ReglaNegocioException("No se puede desactivar un espacio OCUPADO");
        }
        espacio.setActivo(false);
        espacio.setEstado(EstadoEspacio.MANTENIMIENTO);
        espacioRepositorio.save(espacio);
        auditPublisher.publicar(AUDIT_UPDATE, ENTIDAD, espacio);
        sseService.emitir(EVENTO_ACTUALIZADO, maper.toResponseDto(espacio));
    }

    private String generarCodigoEspacio(EspacioRequestDto dto) {
        String tipo = dto.getTipo().name().substring(0, 3); // MOTO->MOT, AUTO->AUT, BUSETA->BUS
        long numero = espacioRepositorio.countByTipoEspacio(dto.getTipo()) + 1;
        String codigo = String.format("ESP-%s-%02d", tipo, numero);
        while (espacioRepositorio.existsByCodigoIgnoreCase(codigo)) {
            numero++;
            codigo = String.format("ESP-%s-%02d", tipo, numero);
        }
        return codigo;
    }
}
