package ec.edu.espe.zonas.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExcepcionesTest {

    @Test
    void recursoNoEncontradoGuardaMensaje() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Zona no encontrada con ID: 123");
        assertEquals("Zona no encontrada con ID: 123", ex.getMessage());
    }

    @Test
    void recursoNoEncontradoEsRuntimeException() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("msg");
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void reglaNegocioGuardaMensaje() {
        ReglaNegocioException ex = new ReglaNegocioException("Capacidad excedida");
        assertEquals("Capacidad excedida", ex.getMessage());
    }

    @Test
    void reglaNegocioEsRuntimeException() {
        ReglaNegocioException ex = new ReglaNegocioException("msg");
        assertTrue(ex instanceof RuntimeException);
    }
}
