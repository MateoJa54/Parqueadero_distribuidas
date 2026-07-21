package ec.edu.espe.zonas.services.Impl;

import ec.edu.espe.zonas.audit.AuditPublisher;
import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.entidades.TipoZona;
import ec.edu.espe.zonas.entidades.Zona;
import ec.edu.espe.zonas.repositorios.EspacioRepositorio;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;
import ec.edu.espe.zonas.utils.RecursoNoEncontradoException;
import ec.edu.espe.zonas.utils.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZonaServicioImplServiceTest {

    private ZonaRepositorio repositorioZona;
    private EspacioRepositorio repositorioEspacio;
    private AuditPublisher auditPublisher;
    private ZonaServicioImpl servicio;

    @BeforeEach
    void setUp() {
        repositorioZona = mock(ZonaRepositorio.class);
        repositorioEspacio = mock(EspacioRepositorio.class);
        auditPublisher = mock(AuditPublisher.class);
        servicio = new ZonaServicioImpl(repositorioZona, repositorioEspacio, auditPublisher);
    }

    // --- helpers ---

    private Zona zona(UUID id, String nombre, boolean activo) {
        return Zona.builder()
                .id(id)
                .nombre(nombre)
                .codigo("ZONA-REG-01")
                .descripcion("desc")
                .tipoZona(TipoZona.REGULAR)
                .activo(activo)
                .capacidad(10)
                .espacios(new ArrayList<>())
                .build();
    }

    private Espacio espacio(UUID id, Zona zona, EstadoEspacio estado, boolean activo) {
        return Espacio.builder()
                .id(id)
                .codigo("E-01")
                .tipoEspacio(TipoEspacio.AUTO)
                .activo(activo)
                .estado(estado)
                .zona(zona)
                .build();
    }

    private ZonaRequestDto request(String nombre) {
        return ZonaRequestDto.builder()
                .nombre(nombre)
                .descripcion("desc")
                .tipo(TipoZona.REGULAR)
                .capacidad(10)
                .build();
    }

    // --- listarZonas ---

    @Test
    @DisplayName("listarZonas retorna lista mapeada")
    void listarZonasOk() {
        Zona z = zona(UUID.randomUUID(), "Zona A", true);
        when(repositorioZona.findAll()).thenReturn(List.of(z));

        List<ZonaRespondeDto> result = servicio.listarZonas();

        assertEquals(1, result.size());
        assertEquals("Zona A", result.get(0).getNombre());
    }

    @Test
    @DisplayName("listarZonas retorna lista vacia")
    void listarZonasVacia() {
        when(repositorioZona.findAll()).thenReturn(List.of());
        assertTrue(servicio.listarZonas().isEmpty());
    }

    // --- obtenerZona ---

    @Test
    @DisplayName("obtenerZona devuelve zona existente")
    void obtenerZonaOk() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona B", true);
        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));

        ZonaRespondeDto dto = servicio.obtenerZona(id);

        assertNotNull(dto);
        assertEquals("Zona B", dto.getNombre());
    }

    @Test
    @DisplayName("obtenerZona lanza RNE si no existe")
    void obtenerZonaNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorioZona.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerZona(id));
    }

    // --- crearZona ---

    @Test
    @DisplayName("crearZona happy path crea y audita")
    void crearZonaOk() {
        ZonaRequestDto req = request("Nueva Zona");
        when(repositorioZona.existsByNombreIgnoreCase("Nueva Zona")).thenReturn(false);
        when(repositorioZona.countByTipoZona(TipoZona.REGULAR)).thenReturn(0L);
        when(repositorioZona.existsByCodigo("ZONA-REG-01")).thenReturn(false);

        ZonaRespondeDto dto = servicio.crearZona(req);

        assertNotNull(dto);
        assertEquals("Nueva Zona", dto.getNombre());
        verify(repositorioZona).save(any(Zona.class));
        verify(auditPublisher).publicar(eq("CREATE"), eq("ZONA"), any());
    }

    @Test
    @DisplayName("crearZona lanza RNE si nombre ya existe")
    void crearZonaNombreDuplicado() {
        ZonaRequestDto req = request("Zona Existente");
        when(repositorioZona.existsByNombreIgnoreCase("Zona Existente")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearZona(req));
        verify(repositorioZona, never()).save(any());
    }

    // --- actualizarZona ---

    @Test
    @DisplayName("actualizarZona happy path actualiza correctamente")
    void actualizarZonaOk() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Vieja Zona", true);
        ZonaRequestDto req = request("Zona Actualizada");

        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioZona.existsByNombreIgnoreCaseAndIdNot("Zona Actualizada", id)).thenReturn(false);
        when(repositorioEspacio.countByZona(z)).thenReturn(5L);

        ZonaRespondeDto dto = servicio.actualizarZona(id, req);

        assertNotNull(dto);
        assertEquals("Zona Actualizada", z.getNombre());
        verify(repositorioZona).save(z);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ZONA"), any());
    }

    @Test
    @DisplayName("actualizarZona lanza RNE si no existe")
    void actualizarZonaNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorioZona.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.actualizarZona(id, request("x")));
    }

    @Test
    @DisplayName("actualizarZona lanza RNE si nombre ya pertenece a otra zona")
    void actualizarZonaNombreDuplicado() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona X", true);
        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioZona.existsByNombreIgnoreCaseAndIdNot("Otro Nombre", id)).thenReturn(true);

        ZonaRequestDto req = request("Otro Nombre");
        assertThrows(ReglaNegocioException.class, () -> servicio.actualizarZona(id, req));
    }

    @Test
    @DisplayName("actualizarZona lanza RNE si capacidad menor a espacios actuales")
    void actualizarZonaCapacidadInsuficiente() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona Y", true);
        ZonaRequestDto req = ZonaRequestDto.builder()
                .nombre("Zona Y")
                .tipo(TipoZona.REGULAR)
                .capacidad(2)
                .build();

        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioZona.existsByNombreIgnoreCaseAndIdNot("Zona Y", id)).thenReturn(false);
        when(repositorioEspacio.countByZona(z)).thenReturn(5L);

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizarZona(id, req));
    }

    // --- activarZona ---

    @Test
    @DisplayName("activarZona activa zona y espacios, audita")
    void activarZonaOk() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona Z", false);
        Espacio e = espacio(UUID.randomUUID(), z, EstadoEspacio.MANTENIMIENTO, false);

        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioEspacio.findByZona(z)).thenReturn(List.of(e));

        servicio.activarZona(id);

        assertTrue(z.isActivo());
        assertTrue(e.isActivo());
        assertEquals(EstadoEspacio.DISPONIBLE, e.getEstado());
        verify(repositorioEspacio).saveAll(List.of(e));
        verify(repositorioZona).save(z);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ZONA"), any());
    }

    @Test
    @DisplayName("activarZona lanza RNE si zona no existe")
    void activarZonaNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorioZona.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.activarZona(id));
    }

    // --- desactivarZona ---

    @Test
    @DisplayName("desactivarZona desactiva zona y espacios, audita")
    void desactivarZonaOk() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona D", true);
        Espacio e = espacio(UUID.randomUUID(), z, EstadoEspacio.DISPONIBLE, true);

        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioEspacio.findByZona(z)).thenReturn(List.of(e));

        servicio.desactivarZona(id);

        assertFalse(z.isActivo());
        assertFalse(e.isActivo());
        assertEquals(EstadoEspacio.MANTENIMIENTO, e.getEstado());
        verify(repositorioEspacio).saveAll(List.of(e));
        verify(repositorioZona).save(z);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ZONA"), any());
    }

    @Test
    @DisplayName("desactivarZona lanza RNE si hay espacios OCUPADOS")
    void desactivarZonaConEspacioOcupado() {
        UUID id = UUID.randomUUID();
        Zona z = zona(id, "Zona E", true);
        Espacio e = espacio(UUID.randomUUID(), z, EstadoEspacio.OCUPADO, true);

        when(repositorioZona.findById(id)).thenReturn(Optional.of(z));
        when(repositorioEspacio.findByZona(z)).thenReturn(List.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivarZona(id));
        verify(repositorioZona, never()).save(any());
    }

    @Test
    @DisplayName("desactivarZona lanza RNE si zona no existe")
    void desactivarZonaNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorioZona.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarZona(id));
    }
}
