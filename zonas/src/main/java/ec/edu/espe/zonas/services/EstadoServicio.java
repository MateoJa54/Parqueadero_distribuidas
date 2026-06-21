package ec.edu.espe.zonas.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;

public interface EstadoServicio {

    List<EspacioRespondeDto> listarEspaciosPorZona(UUID idZona);

    EspacioRequestDto creaEspacio(EspacioRequestDto dto);

    EspacioRequestDto actualizarEstadoEspacio(UUID idEspacio, EspacioRequestDto dto);
    
    void eliminarEspacio(UUID idEspacio);
}
