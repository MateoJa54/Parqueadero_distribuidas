package ec.edu.espe.asignaciones.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassesTest {

    @Test
    void recursoNoEncontradoExceptionMessage() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("not found");
        assertEquals("not found", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void reglaNegocioExceptionMessage() {
        ReglaNegocioException ex = new ReglaNegocioException("business rule");
        assertEquals("business rule", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
