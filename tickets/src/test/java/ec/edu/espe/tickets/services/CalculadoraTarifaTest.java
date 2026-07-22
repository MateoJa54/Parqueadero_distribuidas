package ec.edu.espe.tickets.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ec.edu.espe.tickets.config.TarifaProperties;
import ec.edu.espe.tickets.utils.ReglaNegocioException;

class CalculadoraTarifaTest {

    private CalculadoraTarifa calculadora;
    private OffsetDateTime ingreso;

    @BeforeEach
    void setUp() {
        TarifaProperties properties = new TarifaProperties();
        properties.setMatriz(Map.of("AUTO_AUTO", new BigDecimal("1.50")));
        properties.setPorDefecto(new BigDecimal("2.00"));
        properties.setFactorRol(Map.of("CLIENTE", new BigDecimal("0.60")));
        properties.setFactorRolDefecto(BigDecimal.ONE);
        calculadora = new CalculadoraTarifa(properties);
        ingreso = OffsetDateTime.parse("2026-07-20T08:00:00-05:00");
    }

    @Test
    void cobraUnaHoraMinima() {
        assertEquals(new BigDecimal("1.50"),
                calculadora.calcular("Auto", "AUTO", ingreso, ingreso.plusMinutes(10)));
    }

    @Test
    void redondeaCadaFraccionAlaHoraSiguiente() {
        assertEquals(2, calculadora.horasCobrables(ingreso, ingreso.plusMinutes(61)));
        assertEquals(new BigDecimal("3.00"),
                calculadora.calcular("Auto", "AUTO", ingreso, ingreso.plusMinutes(61)));
    }

    @Test
    void aplicaElFactorDiferenciadoPorRolSinImportarMayusculas() {
        assertEquals(new BigDecimal("1.80"), calculadora.calcular(
                "Auto", "AUTO", " cliente ", ingreso, ingreso.plusMinutes(61)));
    }

    @Test
    void usaValoresPorDefectoParaCombinacionYRolDesconocidos() {
        assertEquals(new BigDecimal("2.00"), calculadora.calcular(
                "Desconocido", "OTRO", "INVITADO", ingreso, ingreso.plusMinutes(1)));
    }

    @Test
    void rechazaUnaSalidaAnteriorAlIngreso() {
        OffsetDateTime salida = ingreso.minusSeconds(1);
        assertThrows(ReglaNegocioException.class,
                () -> calculadora.calcular("Auto", "AUTO", ingreso, salida));
    }

    @Test
    void factorRol_nulo_retornaDefault() {
        assertEquals(BigDecimal.ONE, calculadora.factorRol(null));
    }

    @Test
    void factorRol_blanco_retornaDefault() {
        assertEquals(BigDecimal.ONE, calculadora.factorRol(""));
    }

    @Test
    void horasCobrables_exactamente60min_retornaUnaHora() {
        assertEquals(1, calculadora.horasCobrables(ingreso, ingreso.plusMinutes(60)));
    }

    @Test
    void horasCobrables_exactamente120min_retornasDosHoras() {
        assertEquals(2, calculadora.horasCobrables(ingreso, ingreso.plusMinutes(120)));
    }

    @Test
    void calcular_salidaIgualIngreso_aplicaMinimoDe1Hora() {
        // 0 minutos => ceil(0/60)=0, max(0,1)=1 => minimo 1 hora
        assertEquals(new BigDecimal("1.50"),
                calculadora.calcular("Auto", "AUTO", ingreso, ingreso));
    }
}
