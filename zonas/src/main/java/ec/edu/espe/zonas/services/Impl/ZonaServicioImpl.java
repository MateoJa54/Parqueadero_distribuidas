package ec.edu.espe.zonas.services.Impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.espe.zonas.audit.AuditPublisher;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.Zona;
import ec.edu.espe.zonas.repositorios.EspacioRepositorio;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;
import ec.edu.espe.zonas.services.ZonaServicio;
import ec.edu.espe.zonas.utils.RecursoNoEncontradoException;
import ec.edu.espe.zonas.utils.ReglaNegocioException;
import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ZonaServicioImpl implements ZonaServicio {

    private static final String ENTIDAD = "ZONA";

    private final ZonaRepositorio repositorioZona;
    private final EspacioRepositorio repositorioEspacio;
    private final AuditPublisher auditPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ZonaRespondeDto> listarZonas() {
        return repositorioZona.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ZonaRespondeDto obtenerZona(UUID id) {
        Zona zona = repositorioZona.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada con ID: " + id));
        return mapToDto(zona);
    }

    @Override
    @Transactional
    public ZonaRespondeDto crearZona(ZonaRequestDto request) {

        String nombre = request.getNombre().trim();

        if(repositorioZona.existsByNombreIgnoreCase(nombre)){
            throw new ReglaNegocioException("Ya existe una zona con el nombre: " + nombre);
        }


        Zona objZona = new Zona();

        objZona.setNombre(nombre);
        objZona.setCodigo(generarCodigo(request));
        objZona.setDescripcion(request.getDescripcion());
        objZona.setTipoZona(request.getTipo());
        objZona.setActivo(true);
        objZona.setCapacidad(request.getCapacidad());

        repositorioZona.save(objZona);
        auditPublisher.publicar("CREATE", ENTIDAD, objZona);
        return mapToDto(objZona);
    }

    @Override
    @Transactional
    public ZonaRespondeDto actualizarZona(UUID idZona, ZonaRequestDto request) {
        Zona zona = repositorioZona.findById(idZona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada con ID: " + idZona));

        String nombre = request.getNombre().trim();

        if (repositorioZona.existsByNombreIgnoreCaseAndIdNot(nombre, idZona)) {
            throw new ReglaNegocioException("Ya existe otra zona con el nombre: " + nombre);
        }

        long espaciosActuales = repositorioEspacio.countByZona(zona);
        if (request.getCapacidad() < espaciosActuales) {
            throw new ReglaNegocioException(
                    "La capacidad (" + request.getCapacidad() + ") no puede ser menor a los espacios existentes ("
                            + espaciosActuales + ")");
        }

        zona.setNombre(nombre);
        zona.setDescripcion(request.getDescripcion());
        zona.setTipoZona(request.getTipo());
        zona.setCapacidad(request.getCapacidad());

        repositorioZona.save(zona);
        auditPublisher.publicar("UPDATE", ENTIDAD, zona);
        return mapToDto(zona);
    }

    @Override
    @Transactional
    public void activarZona(UUID idZona) {
        Zona zona = repositorioZona.findById(idZona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada con ID: " + idZona));

        List<Espacio> espacios = repositorioEspacio.findByZona(zona);
        for (Espacio espacio : espacios) {
            espacio.setActivo(true);
            espacio.setEstado(EstadoEspacio.DISPONIBLE);
        }
        repositorioEspacio.saveAll(espacios);

        zona.setActivo(true);
        repositorioZona.save(zona);
        auditPublisher.publicar("UPDATE", ENTIDAD, zona);
    }

    @Override
    @Transactional
    public void desactivarZona(UUID idZona) {
        Zona zona = repositorioZona.findById(idZona)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada con ID: " + idZona));

        List<Espacio> espacios = repositorioEspacio.findByZona(zona);

        //si esta ocuapdo
        boolean hayOcupado = espacios.stream()
                .anyMatch(espacio -> espacio.getEstado() == EstadoEspacio.OCUPADO);
        if (hayOcupado) {
            throw new ReglaNegocioException("No se puede desactivar la zona: tiene espacios OCUPADOS");
        }

        for (Espacio espacio : espacios) {
            espacio.setActivo(false);
            espacio.setEstado(EstadoEspacio.MANTENIMIENTO);
        }
        repositorioEspacio.saveAll(espacios);

        zona.setActivo(false);
        repositorioZona.save(zona);
        auditPublisher.publicar("UPDATE", ENTIDAD, zona);
    }


    private ZonaRespondeDto mapToDto(Zona objZona){
        return ZonaRespondeDto.builder()
        .idZona(objZona.getId())
        .nombre(objZona.getNombre())
        .codigo(objZona.getCodigo())
        .descripcion(objZona.getDescripcion())
        .activo(objZona.isActivo())
        .tipoZona(objZona.getTipoZona())
        .capacidad(objZona.getCapacidad())
        .espacios(objZona.getEspacios().stream().map(this::mapEspacioToDto).toList())
        .fechaCreacion(objZona.getFechaCreacion())
        .fechaActualizacion(objZona.getFechaActualizacion())
        .build();   
    }

    private EspacioRespondeDto mapEspacioToDto(Espacio espacio) {
        return EspacioRespondeDto.builder()
                .id(espacio.getId())
                .codigo(espacio.getCodigo())
                .descripcion(espacio.getDescripcion())
                .tipo(espacio.getTipoEspacio())
                .activo(espacio.isActivo())
                .estado(espacio.getEstado())
                .idZona(espacio.getZona() != null ? espacio.getZona().getId() : null)
                .nombreZona(espacio.getZona() != null ? espacio.getZona().getNombre() : null)
                .build();
    }

    String generarCodigo(ZonaRequestDto request){
        String tipo = request.getTipo().name().substring(0, 3); //REGULAR -> REG, VIP -> VIP
        long numero = repositorioZona.countByTipoZona(request.getTipo()) + 1;
        String codigo = String.format("ZONA-%s-%02d", tipo, numero);
        while (repositorioZona.existsByCodigo(codigo)) {
            numero++;
            codigo = String.format("ZONA-%s-%02d", tipo, numero);
        }
        return codigo;
    }
    
}
