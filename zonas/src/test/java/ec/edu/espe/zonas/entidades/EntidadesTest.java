package ec.edu.espe.zonas.entidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Cubre builders, getters/setters y callbacks JPA de las entidades Zona y Espacio. */
class EntidadesTest {

    @Test
    void zonaBuilderYAccesores() {
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();
        List<Espacio> espacios = new ArrayList<>();

        Zona zona = Zona.builder()
                .id(id)
                .nombre("Zona A")
                .codigo("ZONA-REG-01")
                .descripcion("desc")
                .capacidad(10)
                .espacios(espacios)
                .activo(true)
                .tipoZona(TipoZona.REGULAR)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();

        assertEquals(id, zona.getId());
        assertEquals("Zona A", zona.getNombre());
        assertEquals("ZONA-REG-01", zona.getCodigo());
        assertEquals("desc", zona.getDescripcion());
        assertEquals(10, zona.getCapacidad());
        assertEquals(espacios, zona.getEspacios());
        assertTrue(zona.isActivo());
        assertEquals(TipoZona.REGULAR, zona.getTipoZona());
        assertEquals(ahora, zona.getFechaCreacion());
        assertEquals(ahora, zona.getFechaActualizacion());

        Zona vacia = new Zona();
        vacia.setNombre("N");
        vacia.setActivo(false);
        assertEquals("N", vacia.getNombre());
        assertNotNull(vacia.toString());
        assertTrue(vacia.equals(vacia));
        vacia.hashCode();
    }

    @Test
    void espacioBuilderYAccesores() {
        UUID id = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();
        Zona zona = Zona.builder().id(UUID.randomUUID()).nombre("Z").build();

        Espacio espacio = Espacio.builder()
                .id(id)
                .codigo("E-01")
                .descripcion("desc")
                .tipoEspacio(TipoEspacio.AUTO)
                .activo(true)
                .estado(EstadoEspacio.DISPONIBLE)
                .zona(zona)
                .fechaCreacion(ahora)
                .fechaModificacion(ahora)
                .build();

        assertEquals(id, espacio.getId());
        assertEquals("E-01", espacio.getCodigo());
        assertEquals("desc", espacio.getDescripcion());
        assertEquals(TipoEspacio.AUTO, espacio.getTipoEspacio());
        assertTrue(espacio.isActivo());
        assertEquals(EstadoEspacio.DISPONIBLE, espacio.getEstado());
        assertEquals(zona, espacio.getZona());
        assertEquals(ahora, espacio.getFechaCreacion());
        assertEquals(ahora, espacio.getFechaModificacion());

        Espacio vacio = new Espacio();
        vacio.setCodigo("X");
        vacio.setEstado(EstadoEspacio.OCUPADO);
        assertEquals("X", vacio.getCodigo());
        assertEquals(EstadoEspacio.OCUPADO, vacio.getEstado());
        assertNotNull(vacio.toString());
        assertTrue(vacio.equals(vacio));
        vacio.hashCode();
    }

    @Test
    void callbacksJpaSeteanFechas() {
        Zona zona = new Zona();
        zona.alCrear();
        assertNotNull(zona.getFechaCreacion());
        assertNotNull(zona.getFechaActualizacion());
        LocalDateTime creada = zona.getFechaCreacion();
        zona.alActualizar();
        assertNotNull(zona.getFechaActualizacion());
        assertEquals(creada, zona.getFechaCreacion());

        Espacio espacio = new Espacio();
        espacio.alCrear();
        assertNotNull(espacio.getFechaCreacion());
        assertNotNull(espacio.getFechaModificacion());
        LocalDateTime esCreada = espacio.getFechaCreacion();
        espacio.alActualizar();
        assertNotNull(espacio.getFechaModificacion());
        assertEquals(esCreada, espacio.getFechaCreacion());
    }

    @Test
    void enumsTienenValores() {
        assertEquals(TipoZona.REGULAR, TipoZona.valueOf("REGULAR"));
        assertEquals(TipoEspacio.AUTO, TipoEspacio.valueOf("AUTO"));
        assertEquals(EstadoEspacio.DISPONIBLE, EstadoEspacio.valueOf("DISPONIBLE"));
        assertTrue(TipoZona.values().length >= 4);
        assertTrue(TipoEspacio.values().length == 3);
        assertTrue(EstadoEspacio.values().length == 4);
    }
}
