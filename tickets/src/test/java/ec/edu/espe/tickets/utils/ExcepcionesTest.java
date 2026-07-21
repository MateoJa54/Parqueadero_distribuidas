package ec.edu.espe.tickets.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExcepcionesTest {

    @Test
    void reglaNegocioException_guardaMensaje() {
        ReglaNegocioException ex = new ReglaNegocioException("regla violada");
        assertEquals("regla violada", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void recursoNoEncontradoException_guardaMensaje() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("ticket no existe");
        assertEquals("ticket no existe", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void servicioExternoException_conMensaje() {
        ServicioExternoException ex = new ServicioExternoException("servicio caido");
        assertEquals("servicio caido", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void servicioExternoException_conMensajeYCausa() {
        Throwable causa = new RuntimeException("causa original");
        ServicioExternoException ex = new ServicioExternoException("envuelto", causa);
        assertEquals("envuelto", ex.getMessage());
        assertSame(causa, ex.getCause());
    }
}
