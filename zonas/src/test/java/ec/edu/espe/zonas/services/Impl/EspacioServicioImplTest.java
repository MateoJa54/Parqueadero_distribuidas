package ec.edu.espe.zonas.services.Impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import ec.edu.espe.zonas.sse.SseService;
import ec.edu.espe.zonas.utils.RecursoNoEncontradoException;
import ec.edu.espe.zonas.utils.ReglaNegocioException;
import ec.edu.espe.zonas.utils.UtilMapers;

@ExtendWith(MockitoExtension.class)
class EspacioServicioImplTest {

    @Mock
    private EspacioRepositorio espacioRepositorio;
    @Mock
    private ZonaRepositorio zonaRepositorio;
    @Mock
    private UtilMapers maper;
    @Mock
    private AuditPublisher auditPublisher;
    @Mock
    private SseService sseService;

    @InjectMocks
    private EspacioServicioImpl servicio;

    // ---- helpers ----

    private Zona zona(UUID id, boolean activo, int capacidad) {
        return Zona.builder()
                .id(id)
                .nombre("Zona A")
                .codigo("ZONA-REG-01")
                .capacidad(capacidad)
                .activo(activo)
                .build();
    }

    private Espacio espacio(UUID id, boolean activo, EstadoEspacio estado, Zona zona) {
        return Espacio.builder()
                .id(id)
                .codigo("ESP-AUT-01")
                .descripcion("desc")
                .tipoEspacio(TipoEspacio.AUTO)
                .activo(activo)
                .estado(estado)
                .zona(zona)
                .build();
    }

    private EspacioRequestDto request(UUID idZona) {
        return EspacioRequestDto.builder()
                .idZona(idZona)
                .descripcion("nueva")
                .tipo(TipoEspacio.AUTO)
                .estado(EstadoEspacio.DISPONIBLE)
                .build();
    }

    // ---- obtenerEspacio ----

    @Test
    @DisplayName("obtenerEspacio mapea todos los espacios")
    void obtenerEspacio() {
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findAll()).thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        List<EspacioRespondeDto> res = servicio.obtenerEspacio();

        assertEquals(1, res.size());
    }

    // ---- obtenerEspacioPorId ----

    @Test
    @DisplayName("obtenerEspacioPorId lanza RNE cuando no existe")
    void obtenerEspacioPorIdNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerEspacioPorId(id));
    }

    @Test
    @DisplayName("obtenerEspacioPorId devuelve el dto cuando existe")
    void obtenerEspacioPorIdExiste() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().id(id).build());

        assertEquals(id, servicio.obtenerEspacioPorId(id).getId());
    }

    // ---- crearEspacio ----

    @Test
    @DisplayName("crearEspacio lanza RNE cuando la zona no existe")
    void crearEspacioZonaNoExiste() {
        UUID idZona = UUID.randomUUID();
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crearEspacio(request(idZona)));
    }

    @Test
    @DisplayName("crearEspacio lanza ReglaNegocio cuando la zona esta inactiva")
    void crearEspacioZonaInactiva() {
        UUID idZona = UUID.randomUUID();
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(zona(idZona, false, 10)));

        assertThrows(ReglaNegocioException.class, () -> servicio.crearEspacio(request(idZona)));
    }

    @Test
    @DisplayName("crearEspacio lanza ReglaNegocio cuando se alcanza la capacidad")
    void crearEspacioCapacidadLlena() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 5);
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.countByZona(z)).thenReturn(5L);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearEspacio(request(idZona)));
    }

    @Test
    @DisplayName("crearEspacio guarda, publica y emite en el happy path")
    void crearEspacioHappyPath() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        EspacioRequestDto dto = request(idZona);
        Espacio entidad = espacio(null, true, EstadoEspacio.DISPONIBLE, z);
        Espacio guardado = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, z);

        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.countByZona(z)).thenReturn(2L);
        when(espacioRepositorio.countByTipoEspacio(TipoEspacio.AUTO)).thenReturn(0L);
        when(espacioRepositorio.existsByCodigoIgnoreCase("ESP-AUT-01")).thenReturn(false);
        when(maper.toEntityEspacio(dto)).thenReturn(entidad);
        when(espacioRepositorio.save(any(Espacio.class))).thenReturn(guardado);
        when(maper.toResponseDto(guardado)).thenReturn(EspacioRespondeDto.builder().id(guardado.getId()).build());

        EspacioRespondeDto res = servicio.crearEspacio(dto);

        assertEquals(guardado.getId(), res.getId());
        assertEquals("ESP-AUT-01", entidad.getCodigo());
        verify(auditPublisher).publicar(eq("CREATE"), eq("ESPACIO"), eq(guardado));
        verify(sseService).emitir(eq("espacio-creado"), any());
    }

    @Test
    @DisplayName("generarCodigoEspacio incrementa cuando el codigo ya existe")
    void crearEspacioCodigoColisiona() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        EspacioRequestDto dto = request(idZona);
        Espacio entidad = espacio(null, true, EstadoEspacio.DISPONIBLE, z);
        Espacio guardado = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, z);

        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.countByZona(z)).thenReturn(0L);
        when(espacioRepositorio.countByTipoEspacio(TipoEspacio.AUTO)).thenReturn(0L);
        when(espacioRepositorio.existsByCodigoIgnoreCase("ESP-AUT-01")).thenReturn(true);
        when(espacioRepositorio.existsByCodigoIgnoreCase("ESP-AUT-02")).thenReturn(false);
        when(maper.toEntityEspacio(dto)).thenReturn(entidad);
        when(espacioRepositorio.save(any(Espacio.class))).thenReturn(guardado);
        when(maper.toResponseDto(guardado)).thenReturn(EspacioRespondeDto.builder().build());

        servicio.crearEspacio(dto);

        assertEquals("ESP-AUT-02", entidad.getCodigo());
    }

    // ---- actualizarEspacio ----

    @Test
    @DisplayName("actualizarEspacio lanza RNE cuando no existe")
    void actualizarEspacioNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarEspacio(id, request(UUID.randomUUID())));
    }

    @Test
    @DisplayName("actualizarEspacio actualiza descripcion y tipo, publica y emite")
    void actualizarEspacioHappyPath() {
        UUID id = UUID.randomUUID();
        Zona z = zona(UUID.randomUUID(), true, 10);
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, z);
        EspacioRequestDto dto = EspacioRequestDto.builder()
                .idZona(z.getId()).descripcion("cambio").tipo(TipoEspacio.MOTO).build();

        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));
        when(espacioRepositorio.save(e)).thenReturn(e);
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        servicio.actualizarEspacio(id, dto);

        assertEquals("cambio", e.getDescripcion());
        assertEquals(TipoEspacio.MOTO, e.getTipoEspacio());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ESPACIO"), eq(e));
        verify(sseService).emitir(eq("espacio-actualizado"), any());
    }

    // ---- cambiarEstado ----

    @Test
    @DisplayName("cambiarEstado lanza IAE cuando el id es null")
    void cambiarEstadoIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarEstado(null, EstadoEspacio.OCUPADO));
    }

    @Test
    @DisplayName("cambiarEstado lanza IAE cuando el estado es null")
    void cambiarEstadoEstadoNull() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.cambiarEstado(UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("cambiarEstado lanza RNE cuando no existe")
    void cambiarEstadoNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.cambiarEstado(id, EstadoEspacio.OCUPADO));
    }

    @Test
    @DisplayName("cambiarEstado lanza ReglaNegocio cuando el espacio esta inactivo")
    void cambiarEstadoInactivo() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, false, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.cambiarEstado(id, EstadoEspacio.OCUPADO));
    }

    @Test
    @DisplayName("cambiarEstado lanza ReglaNegocio cuando el estado no cambia")
    void cambiarEstadoMismoEstado() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.cambiarEstado(id, EstadoEspacio.DISPONIBLE));
    }

    @Test
    @DisplayName("cambiarEstado guarda, publica y emite en el happy path")
    void cambiarEstadoHappyPath() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));
        when(espacioRepositorio.save(e)).thenReturn(e);
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        servicio.cambiarEstado(id, EstadoEspacio.OCUPADO);

        assertEquals(EstadoEspacio.OCUPADO, e.getEstado());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ESPACIO"), eq(e));
        verify(sseService).emitir(eq("espacio-actualizado"), any());
    }

    // ---- obtenerEspacioPorEstado ----

    @Test
    @DisplayName("obtenerEspacioPorEstado lanza IAE cuando el estado es null")
    void obtenerEspacioPorEstadoNull() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEspacioPorEstado(null));
    }

    @Test
    @DisplayName("obtenerEspacioPorEstado mapea resultados")
    void obtenerEspacioPorEstadoOk() {
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.OCUPADO, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findByEstado(EstadoEspacio.OCUPADO)).thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        assertEquals(1, servicio.obtenerEspacioPorEstado(EstadoEspacio.OCUPADO).size());
    }

    // ---- obtenerEspacioPorZonaEstado ----

    @Test
    @DisplayName("obtenerEspacioPorZonaEstado lanza IAE cuando el estado es null")
    void obtenerEspacioPorZonaEstadoNull() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerEspacioPorZonaEstado(UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("obtenerEspacioPorZonaEstado lanza RNE cuando la zona no existe")
    void obtenerEspacioPorZonaEstadoZonaNoExiste() {
        UUID idZona = UUID.randomUUID();
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerEspacioPorZonaEstado(idZona, EstadoEspacio.DISPONIBLE));
    }

    @Test
    @DisplayName("obtenerEspacioPorZonaEstado lanza RNE cuando no hay espacios")
    void obtenerEspacioPorZonaEstadoSinEspacios() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.findByZonaAndEstado(z, EstadoEspacio.DISPONIBLE)).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerEspacioPorZonaEstado(idZona, EstadoEspacio.DISPONIBLE));
    }

    @Test
    @DisplayName("obtenerEspacioPorZonaEstado devuelve el primero cuando hay espacios")
    void obtenerEspacioPorZonaEstadoOk() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, z);
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.findByZonaAndEstado(z, EstadoEspacio.DISPONIBLE)).thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().id(e.getId()).build());

        assertEquals(e.getId(), servicio.obtenerEspacioPorZonaEstado(idZona, EstadoEspacio.DISPONIBLE).getId());
    }

    // ---- listarDisponibles ----

    @Test
    @DisplayName("listarDisponibles con zona y tipo filtra por ambos y por activo")
    void listarDisponiblesZonaYTipo() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        Espacio activo = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, z);
        Espacio inactivo = espacio(UUID.randomUUID(), false, EstadoEspacio.DISPONIBLE, z);
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.findByZonaAndTipoEspacioAndEstado(z, TipoEspacio.AUTO, EstadoEspacio.DISPONIBLE))
                .thenReturn(List.of(activo, inactivo));
        when(maper.toResponseDto(activo)).thenReturn(EspacioRespondeDto.builder().build());

        assertEquals(1, servicio.listarDisponibles(idZona, TipoEspacio.AUTO).size());
    }

    @Test
    @DisplayName("listarDisponibles con zona sin tipo usa findByZonaAndEstado")
    void listarDisponiblesSoloZona() {
        UUID idZona = UUID.randomUUID();
        Zona z = zona(idZona, true, 10);
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, z);
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(z));
        when(espacioRepositorio.findByZonaAndEstado(z, EstadoEspacio.DISPONIBLE)).thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        assertEquals(1, servicio.listarDisponibles(idZona, null).size());
    }

    @Test
    @DisplayName("listarDisponibles con zona inexistente lanza RNE")
    void listarDisponiblesZonaNoExiste() {
        UUID idZona = UUID.randomUUID();
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.listarDisponibles(idZona, TipoEspacio.AUTO));
    }

    @Test
    @DisplayName("listarDisponibles solo tipo usa findByTipoEspacioAndEstado")
    void listarDisponiblesSoloTipo() {
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findByTipoEspacioAndEstado(TipoEspacio.MOTO, EstadoEspacio.DISPONIBLE))
                .thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        assertEquals(1, servicio.listarDisponibles(null, TipoEspacio.MOTO).size());
    }

    @Test
    @DisplayName("listarDisponibles sin filtros usa findByEstado")
    void listarDisponiblesSinFiltros() {
        Espacio e = espacio(UUID.randomUUID(), true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findByEstado(EstadoEspacio.DISPONIBLE)).thenReturn(List.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        assertEquals(1, servicio.listarDisponibles(null, null).size());
    }

    // ---- verificarDisponibilidad ----

    @Test
    @DisplayName("verificarDisponibilidad lanza RNE cuando no existe")
    void verificarDisponibilidadNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.verificarDisponibilidad(id));
    }

    @Test
    @DisplayName("verificarDisponibilidad true cuando activo y DISPONIBLE")
    void verificarDisponibilidadDisponible() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        DisponibilidadResponseDto res = servicio.verificarDisponibilidad(id);

        assertTrue(res.isDisponible());
        assertEquals(id, res.getIdEspacio());
    }

    @Test
    @DisplayName("verificarDisponibilidad false cuando OCUPADO")
    void verificarDisponibilidadOcupado() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.OCUPADO, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertFalse(servicio.verificarDisponibilidad(id).isDisponible());
    }

    // ---- activarEspacio ----

    @Test
    @DisplayName("activarEspacio lanza RNE cuando no existe")
    void activarEspacioNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.activarEspacio(id));
    }

    @Test
    @DisplayName("activarEspacio lanza ReglaNegocio cuando ya esta activo")
    void activarEspacioYaActivo() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.activarEspacio(id));
    }

    @Test
    @DisplayName("activarEspacio lanza ReglaNegocio cuando la zona esta inactiva")
    void activarEspacioZonaInactiva() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, false, EstadoEspacio.MANTENIMIENTO, zona(UUID.randomUUID(), false, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.activarEspacio(id));
    }

    @Test
    @DisplayName("activarEspacio activa, guarda, publica y emite")
    void activarEspacioHappyPath() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, false, EstadoEspacio.MANTENIMIENTO, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        servicio.activarEspacio(id);

        assertTrue(e.isActivo());
        assertEquals(EstadoEspacio.DISPONIBLE, e.getEstado());
        verify(espacioRepositorio).save(e);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ESPACIO"), eq(e));
        verify(sseService).emitir(eq("espacio-actualizado"), any());
    }

    // ---- desactivarEspacio ----

    @Test
    @DisplayName("desactivarEspacio lanza RNE cuando no existe")
    void desactivarEspacioNoExiste() {
        UUID id = UUID.randomUUID();
        when(espacioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarEspacio(id));
    }

    @Test
    @DisplayName("desactivarEspacio lanza ReglaNegocio cuando ya esta inactivo")
    void desactivarEspacioYaInactivo() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, false, EstadoEspacio.MANTENIMIENTO, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivarEspacio(id));
    }

    @Test
    @DisplayName("desactivarEspacio lanza ReglaNegocio cuando el espacio esta OCUPADO")
    void desactivarEspacioOcupado() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.OCUPADO, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivarEspacio(id));
        verify(espacioRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("desactivarEspacio desactiva, guarda, publica y emite")
    void desactivarEspacioHappyPath() {
        UUID id = UUID.randomUUID();
        Espacio e = espacio(id, true, EstadoEspacio.DISPONIBLE, zona(UUID.randomUUID(), true, 10));
        when(espacioRepositorio.findById(id)).thenReturn(Optional.of(e));
        when(maper.toResponseDto(e)).thenReturn(EspacioRespondeDto.builder().build());

        servicio.desactivarEspacio(id);

        assertFalse(e.isActivo());
        assertEquals(EstadoEspacio.MANTENIMIENTO, e.getEstado());
        verify(espacioRepositorio).save(e);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ESPACIO"), eq(e));
        verify(sseService).emitir(eq("espacio-actualizado"), any());
    }
}
