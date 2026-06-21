package ec.edu.espe.zonas.repositorios;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.entidades.Zona;

public interface EspacioRepositorio extends JpaRepository<Espacio,UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    long countByZona(Zona zona);

    long countByTipoEspacio(ec.edu.espe.zonas.entidades.TipoEspacio tipoEspacio);

    List<Espacio> findByZona(Zona zona);

    List<Espacio> findByZonaAndEstado(Zona zona, EstadoEspacio estado);
    
    List<Espacio> findByEstado(EstadoEspacio estado);

    // Consultas de disponibilidad filtrables por tipo y/o zona.
    List<Espacio> findByTipoEspacioAndEstado(TipoEspacio tipoEspacio, EstadoEspacio estado);

    List<Espacio> findByZonaAndTipoEspacioAndEstado(Zona zona, TipoEspacio tipoEspacio, EstadoEspacio estado);

}