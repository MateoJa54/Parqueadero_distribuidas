
package ec.edu.espe.zonas.repositorios;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.espe.zonas.entidades.TipoZona;
import ec.edu.espe.zonas.entidades.Zona;

public interface ZonaRepositorio extends JpaRepository<Zona, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    boolean existsByCodigo(String codigo);

    long countByTipoZona(TipoZona tipoZona);
}
