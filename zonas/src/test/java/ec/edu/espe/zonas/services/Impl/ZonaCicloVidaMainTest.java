package ec.edu.espe.zonas.services.Impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import ec.edu.espe.zonas.ZonasApplication;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.Zona;
import ec.edu.espe.zonas.repositorios.EspacioRepositorio;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;

/** Ejercita el escenario demostrativo del ciclo de vida usando dependencias simuladas. */
class ZonaCicloVidaMainTest {

    @Test
    void mainEjecutaFlujoCompletoYCierraContexto() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ZonaServicioImpl zonaServicio = mock(ZonaServicioImpl.class);
        EspacioServicioImpl espacioServicio = mock(EspacioServicioImpl.class);
        ZonaRepositorio zonaRepositorio = mock(ZonaRepositorio.class);
        EspacioRepositorio espacioRepositorio = mock(EspacioRepositorio.class);

        when(context.getBean(ZonaServicioImpl.class)).thenReturn(zonaServicio);
        when(context.getBean(EspacioServicioImpl.class)).thenReturn(espacioServicio);
        when(context.getBean(ZonaRepositorio.class)).thenReturn(zonaRepositorio);
        when(context.getBean(EspacioRepositorio.class)).thenReturn(espacioRepositorio);

        UUID idZona = UUID.randomUUID();
        ZonaRespondeDto zonaDto = ZonaRespondeDto.builder()
                .idZona(idZona).codigo("ZONA-01").capacidad(2).build();
        when(zonaServicio.crearZona(any())).thenReturn(zonaDto);

        // Dos creaciones OK y la tercera lanza excepcion (se captura en crearEspacio).
        when(espacioServicio.crearEspacio(any()))
                .thenReturn(EspacioRespondeDto.builder().codigo("ESP-A").build())
                .thenReturn(EspacioRespondeDto.builder().codigo("ESP-B").build())
                .thenThrow(new RuntimeException("capacidad excedida"));

        Zona zona = Zona.builder().id(idZona).nombre("Z").activo(true).build();
        Espacio espacio = Espacio.builder()
                .id(UUID.randomUUID()).codigo("ESP-A")
                .estado(EstadoEspacio.DISPONIBLE).activo(true).build();
        when(zonaRepositorio.findById(idZona)).thenReturn(Optional.of(zona));
        when(espacioRepositorio.findByZona(zona)).thenReturn(List.of(espacio));

        // Primer intento de desactivar (espacio OCUPADO) -> lanza, se captura en el main.
        doThrow(new RuntimeException("zona con espacio ocupado"))
                .doNothing()
                .when(zonaServicio).desactivarZona(idZona);

        try (MockedStatic<SpringApplication> mockSpring = mockStatic(SpringApplication.class)) {
            mockSpring.when(() -> SpringApplication.run(ZonasApplication.class, new String[] {}))
                    .thenReturn(context);

            ZonaCicloVidaMain.main(new String[] {});
        }

        verify(zonaServicio).crearZona(any());
        verify(zonaServicio).activarZona(idZona);
        verify(context).close();
    }
}
