package ec.edu.espe.zonas.controlller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.entidades.TipoZona;
import ec.edu.espe.zonas.services.ZonaServicio;

class ZonaControllerTest {

    private ZonaServicio zonaServicio;
    private ZonaController controller;

    @BeforeEach
    void setUp() {
        zonaServicio = mock(ZonaServicio.class);
        controller = new ZonaController(zonaServicio);
    }

    @Test
    void listarZonasRetornaOk() {
        ZonaRespondeDto dto = ZonaRespondeDto.builder().nombre("Z1").build();
        when(zonaServicio.listarZonas()).thenReturn(List.of(dto));

        ResponseEntity<List<ZonaRespondeDto>> response = controller.listarZonas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listarZonasListaVaciaRetornaOk() {
        when(zonaServicio.listarZonas()).thenReturn(List.of());

        ResponseEntity<List<ZonaRespondeDto>> response = controller.listarZonas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void obtenerZonaRetornaOk() {
        UUID id = UUID.randomUUID();
        ZonaRespondeDto dto = ZonaRespondeDto.builder().idZona(id).build();
        when(zonaServicio.obtenerZona(id)).thenReturn(dto);

        ResponseEntity<ZonaRespondeDto> response = controller.obtenerZona(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getIdZona());
    }

    @Test
    void crearZonaRetorna201() {
        ZonaRequestDto request = ZonaRequestDto.builder()
                .nombre("Zona A").tipo(TipoZona.REGULAR).capacidad(10).build();
        ZonaRespondeDto dto = ZonaRespondeDto.builder().nombre("Zona A").build();
        when(zonaServicio.crearZona(request)).thenReturn(dto);

        ResponseEntity<ZonaRespondeDto> response = controller.crearZona(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Zona A", response.getBody().getNombre());
    }

    @Test
    void actualizarZonaRetornaOk() {
        UUID id = UUID.randomUUID();
        ZonaRequestDto request = ZonaRequestDto.builder()
                .nombre("Zona B").tipo(TipoZona.VIP).capacidad(5).build();
        ZonaRespondeDto dto = ZonaRespondeDto.builder().nombre("Zona B").build();
        when(zonaServicio.actualizarZona(id, request)).thenReturn(dto);

        ResponseEntity<ZonaRespondeDto> response = controller.actualizarZona(id, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Zona B", response.getBody().getNombre());
    }

    @Test
    void activarZonaRetornaNoContent() {
        UUID id = UUID.randomUUID();
        doNothing().when(zonaServicio).activarZona(id);

        ResponseEntity<Void> response = controller.activarZona(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(zonaServicio).activarZona(id);
    }

    @Test
    void desactivarZonaRetornaNoContent() {
        UUID id = UUID.randomUUID();
        doNothing().when(zonaServicio).desactivarZona(id);

        ResponseEntity<Void> response = controller.desactivarZona(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(zonaServicio).desactivarZona(id);
    }
}
