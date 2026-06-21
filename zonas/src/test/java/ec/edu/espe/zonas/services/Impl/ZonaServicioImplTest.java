package ec.edu.espe.zonas.services.Impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.entidades.TipoZona;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;

@ExtendWith(MockitoExtension.class)
class ZonaServicioImplTest {

    @Mock
    private ZonaRepositorio repositorioZona;

    @InjectMocks
    private ZonaServicioImpl zonaServicio;

    @Test
    void generarCodigoPrimeraZonaConBddVacia() {
        when(repositorioZona.countByTipoZona(TipoZona.REGULAR)).thenReturn(0L);
        when(repositorioZona.existsByCodigo("ZONA-REG-01")).thenReturn(false);

        ZonaRequestDto request = ZonaRequestDto.builder()
                .nombre("Zona Regular")
                .tipo(TipoZona.REGULAR)
                .build();

        String codigo = zonaServicio.generarCodigo(request);

        // bdd vacia -> primera zona -> 01
        assertEquals("ZONA-REG-01", codigo);
    }

    @Test
    void generarCodigoPrimeraZonaVipEmpiezaEn01() {
        when(repositorioZona.countByTipoZona(TipoZona.VIP)).thenReturn(0L);
        when(repositorioZona.existsByCodigo("ZONA-VIP-01")).thenReturn(false);

        ZonaRequestDto request = ZonaRequestDto.builder()
                .nombre("Zona VIP")
                .tipo(TipoZona.VIP)
                .build();

        String codigo = zonaServicio.generarCodigo(request);

        // VIP tambien inicia en 01 si no hay VIP previas
        assertEquals("ZONA-VIP-01", codigo);
    }
}
