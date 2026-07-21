package ec.edu.espe.zonas.controlller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ec.edu.espe.zonas.dtos.DisponibilidadResponseDto;
import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.services.EspacioServicio;

class EspacioControllerTest {

    private EspacioServicio espacioServicio;
    private EspacioController controller;

    @BeforeEach
    void setUp() {
        espacioServicio = mock(EspacioServicio.class);
        controller = new EspacioController(espacioServicio);
    }

    @Test
    void listarEspaciosRetornaOk() {
        EspacioRespondeDto dto = EspacioRespondeDto.builder().codigo("ESP-AUT-01").build();
        when(espacioServicio.obtenerEspacio()).thenReturn(List.of(dto));

        ResponseEntity<List<EspacioRespondeDto>> response = controller.listarEspacios();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listarEspaciosVacioRetornaOk() {
        when(espacioServicio.obtenerEspacio()).thenReturn(List.of());

        ResponseEntity<List<EspacioRespondeDto>> response = controller.listarEspacios();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void obtenerEspacioRetornaOk() {
        UUID id = UUID.randomUUID();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().id(id).build();
        when(espacioServicio.obtenerEspacioPorId(id)).thenReturn(dto);

        ResponseEntity<EspacioRespondeDto> response = controller.obtenerEspacio(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void crearEspacioRetorna201() {
        UUID zonaId = UUID.randomUUID();
        EspacioRequestDto request = EspacioRequestDto.builder()
                .idZona(zonaId).tipo(TipoEspacio.AUTO).build();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().codigo("ESP-AUT-01").build();
        when(espacioServicio.crearEspacio(request)).thenReturn(dto);

        ResponseEntity<EspacioRespondeDto> response = controller.crearEspacio(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("ESP-AUT-01", response.getBody().getCodigo());
    }

    @Test
    void actualizarEspacioRetornaOk() {
        UUID id = UUID.randomUUID();
        EspacioRequestDto request = EspacioRequestDto.builder()
                .idZona(UUID.randomUUID()).tipo(TipoEspacio.MOTO).build();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().codigo("ESP-MOT-01").build();
        when(espacioServicio.actualizarEspacio(id, request)).thenReturn(dto);

        ResponseEntity<EspacioRespondeDto> response = controller.actualizarEspacio(id, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ESP-MOT-01", response.getBody().getCodigo());
    }

    @Test
    void cambiarEstadoRetornaOk() {
        UUID id = UUID.randomUUID();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().estado(EstadoEspacio.OCUPADO).build();
        when(espacioServicio.cambiarEstado(id, EstadoEspacio.OCUPADO)).thenReturn(dto);

        ResponseEntity<EspacioRespondeDto> response = controller.cambiarEstado(id, EstadoEspacio.OCUPADO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EstadoEspacio.OCUPADO, response.getBody().getEstado());
    }

    @Test
    void listarPorEstadoRetornaOk() {
        EspacioRespondeDto dto = EspacioRespondeDto.builder().estado(EstadoEspacio.DISPONIBLE).build();
        when(espacioServicio.obtenerEspacioPorEstado(EstadoEspacio.DISPONIBLE)).thenReturn(List.of(dto));

        ResponseEntity<List<EspacioRespondeDto>> response = controller.listarPorEstado(EstadoEspacio.DISPONIBLE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void listarDisponiblesSinFiltrosRetornaOk() {
        when(espacioServicio.listarDisponibles(null, null)).thenReturn(List.of());

        ResponseEntity<List<EspacioRespondeDto>> response = controller.listarDisponibles(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listarDisponiblesConZonaYTipoRetornaOk() {
        UUID zonaId = UUID.randomUUID();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().build();
        when(espacioServicio.listarDisponibles(zonaId, TipoEspacio.AUTO)).thenReturn(List.of(dto));

        ResponseEntity<List<EspacioRespondeDto>> response = controller.listarDisponibles(zonaId, TipoEspacio.AUTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void verificarDisponibilidadRetornaOk() {
        UUID id = UUID.randomUUID();
        DisponibilidadResponseDto dto = DisponibilidadResponseDto.builder()
                .idEspacio(id).disponible(true).build();
        when(espacioServicio.verificarDisponibilidad(id)).thenReturn(dto);

        ResponseEntity<DisponibilidadResponseDto> response = controller.verificarDisponibilidad(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isDisponible());
    }

    @Test
    void buscarPorZonaYEstadoRetornaOk() {
        UUID zonaId = UUID.randomUUID();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().estado(EstadoEspacio.DISPONIBLE).build();
        when(espacioServicio.obtenerEspacioPorZonaEstado(zonaId, EstadoEspacio.DISPONIBLE)).thenReturn(dto);

        ResponseEntity<EspacioRespondeDto> response = controller.buscarPorZonaYEstado(zonaId, EstadoEspacio.DISPONIBLE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void activarEspacioRetornaNoContent() {
        UUID id = UUID.randomUUID();
        doNothing().when(espacioServicio).activarEspacio(id);

        ResponseEntity<Void> response = controller.activarEspacio(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(espacioServicio).activarEspacio(id);
    }

    @Test
    void desactivarEspacioRetornaNoContent() {
        UUID id = UUID.randomUUID();
        doNothing().when(espacioServicio).desactivarEspacio(id);

        ResponseEntity<Void> response = controller.desactivarEspacio(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(espacioServicio).desactivarEspacio(id);
    }
}
