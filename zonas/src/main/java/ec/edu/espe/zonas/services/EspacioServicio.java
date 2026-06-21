package ec.edu.espe.zonas.services;

import java.util.List;
import java.util.UUID;

import ec.edu.espe.zonas.dtos.DisponibilidadResponseDto;
import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
public interface EspacioServicio {

    List<EspacioRespondeDto> obtenerEspacio();

    EspacioRespondeDto obtenerEspacioPorId(UUID idEspacio);

    EspacioRespondeDto crearEspacio(EspacioRequestDto dto);

    EspacioRespondeDto actualizarEspacio(UUID idEspacio, EspacioRequestDto dto);

    void eliminarEspacio(UUID idEspacio);

    EspacioRespondeDto cambiarEstado(UUID idEspacio, EstadoEspacio estado);

    void activarEspacio(UUID idEspacio);

    void desactivarEspacio(UUID idEspacio);

    List<EspacioRespondeDto> obtnerEspacioPOrEstado(EstadoEspacio estado);

    EspacioRespondeDto obtenerEspacioPorZonaEstado(UUID idZona, EstadoEspacio estado);

    // Disponibilidad pensada para el sistema de tickets.
    List<EspacioRespondeDto> listarDisponibles(UUID idZona, TipoEspacio tipo);

    DisponibilidadResponseDto verificarDisponibilidad(UUID idEspacio);
}
