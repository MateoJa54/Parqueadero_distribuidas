package ec.edu.espe.zonas.utils;

import org.springframework.stereotype.Component;

import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;

@Component
public class UtilMapers {

    public EspacioRespondeDto toResponseDto(Espacio objEspacio) {
        if (objEspacio == null) {
            return null;
        }
        return EspacioRespondeDto.builder()
                .id(objEspacio.getId())
                .codigo(objEspacio.getCodigo())
                .descripcion(objEspacio.getDescripcion())
                .tipo(objEspacio.getTipoEspacio())
                .activo(objEspacio.isActivo())
                .estado(objEspacio.getEstado())
                .idZona(objEspacio.getZona() != null ? objEspacio.getZona().getId() : null)
                .nombreZona(objEspacio.getZona() != null ? objEspacio.getZona().getNombre() : null)
                .build();
    }

    public Espacio toEntityEspacio(EspacioRequestDto request) {
        if (request == null) {
            return null;
        }
        return Espacio.builder()
                .descripcion(request.getDescripcion())
                .tipoEspacio(request.getTipo())
                .activo(true) // Por defecto, el espacio se crea como activo
                .estado(request.getEstado())
                .build();
    }

}