package ec.edu.espe.zonas.sse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.services.EspacioServicio;

class SseControllerTest {

    private SseService sseService;
    private EspacioServicio espacioServicio;
    private SseController controller;

    @BeforeEach
    void setUp() {
        sseService = mock(SseService.class);
        espacioServicio = mock(EspacioServicio.class);
        controller = new SseController(sseService, espacioServicio);
    }

    @Test
    void streamEspaciosRetornaEmitter() {
        SseEmitter emitter = mock(SseEmitter.class);
        when(sseService.nuevaConexion()).thenReturn(emitter);
        when(espacioServicio.obtenerEspacio()).thenReturn(List.of());

        SseEmitter result = controller.streamEspacios();

        assertNotNull(result);
        assertSame(emitter, result);
        verify(sseService).nuevaConexion();
        verify(sseService).enviarEvento(eq(emitter), eq("snapshot"), any());
    }

    @Test
    void streamEspaciosEnviaSnapshotInicial() {
        SseEmitter emitter = new SseEmitter();
        EspacioRespondeDto dto = EspacioRespondeDto.builder().codigo("ESP-AUT-01").build();
        when(sseService.nuevaConexion()).thenReturn(emitter);
        when(espacioServicio.obtenerEspacio()).thenReturn(List.of(dto));

        SseEmitter result = controller.streamEspacios();

        assertNotNull(result);
        verify(espacioServicio).obtenerEspacio();
    }
}
