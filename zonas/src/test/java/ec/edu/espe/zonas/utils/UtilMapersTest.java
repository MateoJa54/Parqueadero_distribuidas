package ec.edu.espe.zonas.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.entidades.Zona;

class UtilMapersTest {

    private UtilMapers mapper;

    @BeforeEach
    void setUp() {
        mapper = new UtilMapers();
    }

    @Test
    void toResponseDtoConEspacioCompleto() {
        UUID espacioId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        Zona zona = new Zona();
        zona.setId(zonaId);
        zona.setNombre("Zona A");

        Espacio espacio = Espacio.builder()
                .id(espacioId)
                .codigo("ESP-AUT-01")
                .descripcion("Espacio auto")
                .tipoEspacio(TipoEspacio.AUTO)
                .activo(true)
                .estado(EstadoEspacio.DISPONIBLE)
                .zona(zona)
                .build();

        EspacioRespondeDto dto = mapper.toResponseDto(espacio);

        assertNotNull(dto);
        assertEquals(espacioId, dto.getId());
        assertEquals("ESP-AUT-01", dto.getCodigo());
        assertEquals("Espacio auto", dto.getDescripcion());
        assertEquals(TipoEspacio.AUTO, dto.getTipo());
        assertTrue(dto.isActivo());
        assertEquals(EstadoEspacio.DISPONIBLE, dto.getEstado());
        assertEquals(zonaId, dto.getIdZona());
        assertEquals("Zona A", dto.getNombreZona());
    }

    @Test
    void toResponseDtoConZonaNulaRetornaNullsEnZona() {
        Espacio espacio = Espacio.builder()
                .id(UUID.randomUUID())
                .codigo("ESP-MOT-01")
                .tipoEspacio(TipoEspacio.MOTO)
                .activo(false)
                .estado(EstadoEspacio.MANTENIMIENTO)
                .zona(null)
                .build();

        EspacioRespondeDto dto = mapper.toResponseDto(espacio);

        assertNotNull(dto);
        assertNull(dto.getIdZona());
        assertNull(dto.getNombreZona());
    }

    @Test
    void toResponseDtoConEspacioNuloRetornaNull() {
        EspacioRespondeDto dto = mapper.toResponseDto(null);
        assertNull(dto);
    }

    @Test
    void toEntityEspacioConRequestCompleto() {
        UUID zonaId = UUID.randomUUID();
        EspacioRequestDto request = EspacioRequestDto.builder()
                .idZona(zonaId)
                .descripcion("Espacio moto")
                .tipo(TipoEspacio.MOTO)
                .estado(EstadoEspacio.DISPONIBLE)
                .build();

        Espacio espacio = mapper.toEntityEspacio(request);

        assertNotNull(espacio);
        assertEquals("Espacio moto", espacio.getDescripcion());
        assertEquals(TipoEspacio.MOTO, espacio.getTipoEspacio());
        assertTrue(espacio.isActivo());
        assertEquals(EstadoEspacio.DISPONIBLE, espacio.getEstado());
    }

    @Test
    void toEntityEspacioConRequestNuloRetornaNull() {
        Espacio espacio = mapper.toEntityEspacio(null);
        assertNull(espacio);
    }

    @Test
    void toEntityEspacioActivoPorDefecto() {
        EspacioRequestDto request = EspacioRequestDto.builder()
                .idZona(UUID.randomUUID())
                .tipo(TipoEspacio.AUTO)
                .build();

        Espacio espacio = mapper.toEntityEspacio(request);

        assertTrue(espacio.isActivo());
    }
}
