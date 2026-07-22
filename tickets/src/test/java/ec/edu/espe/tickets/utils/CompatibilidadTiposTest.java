package ec.edu.espe.tickets.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompatibilidadTiposTest {

    @Test
    void normalizaMayusculasTildesYEspacios() {
        assertEquals("MOTOCICLETA", CompatibilidadTipos.normalizar("  Motocicleta  "));
        assertEquals("CAMION", CompatibilidadTipos.normalizar("Camión"));
        assertEquals("", CompatibilidadTipos.normalizar(null));
    }

    @Test
    void aplicaLaMatrizEstrictaDeCompatibilidad() {
        assertTrue(CompatibilidadTipos.sonCompatibles("Auto", "AUTO"));
        assertTrue(CompatibilidadTipos.sonCompatibles("Motocicleta", "moto"));
        assertTrue(CompatibilidadTipos.sonCompatibles("Camioneta", "BUSETA"));
        assertFalse(CompatibilidadTipos.sonCompatibles("Auto", "MOTO"));
        assertFalse(CompatibilidadTipos.sonCompatibles("Desconocido", "AUTO"));
    }

    @Test
    void construyeLaClaveNormalizadaDeTarifa() {
        assertEquals("MOTOCICLETA_MOTO",
                CompatibilidadTipos.claveTarifa("Motocicleta", " moto "));
    }
}
