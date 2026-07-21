package ec.edu.espe.zonas.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleRecursoNoEncontradoRetorna404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No encontrado");

        ResponseEntity<Map<String, Object>> response = handler.handleNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("No encontrado", response.getBody().get("mensaje"));
    }

    @Test
    void handleReglaNegocioRetorna409() {
        ReglaNegocioException ex = new ReglaNegocioException("Conflicto de negocio");

        ResponseEntity<Map<String, Object>> response = handler.handleReglaNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Conflicto de negocio", response.getBody().get("mensaje"));
    }

    @Test
    void handleIllegalArgumentRetorna400() {
        IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argumento inválido", response.getBody().get("mensaje"));
    }

    @Test
    void handleAccessDeniedRetorna403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tiene permisos para esta operacion", response.getBody().get("mensaje"));
    }

    @Test
    void handleRuntimeExceptionRetorna500() {
        RuntimeException ex = new RuntimeException("Error inesperado interno");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error inesperado interno", response.getBody().get("mensaje"));
    }

    @Test
    void handleRuntimeExceptionMensajeNuloRetorna500ConDefault() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error inesperado", response.getBody().get("mensaje"));
    }

    @Test
    void handleValidationRetorna400ConErrores() {
        // Construct MethodArgumentNotValidException using binding result
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "nombre", "El nombre es obligatorio"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().get("errores"));
    }

    @Test
    void baseBodyContieneCamposEsperados() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("test");

        ResponseEntity<Map<String, Object>> response = handler.handleNoEncontrado(ex);

        Map<String, Object> body = response.getBody();
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("mensaje"));
    }

    @Test
    void handleTypeMismatchRetorna400ConValorYParametro() {
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex =
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                        "abc", Integer.class, "idZona", null, new RuntimeException("nope"));

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String mensaje = (String) response.getBody().get("mensaje");
        assertTrue(mensaje.contains("abc"));
        assertTrue(mensaje.contains("idZona"));
    }

    @Test
    void handleNotReadableCuerpoGenericoRetorna400() {
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "roto", new RuntimeException("x"),
                        new org.springframework.mock.http.MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El cuerpo de la peticion es invalido o esta mal formado",
                response.getBody().get("mensaje"));
    }

    @Test
    void handleNotReadableEnumInvalidoListaValoresPermitidos() {
        com.fasterxml.jackson.databind.exc.InvalidFormatException ife =
                com.fasterxml.jackson.databind.exc.InvalidFormatException.from(
                        null, "valor no valido", "MORADO",
                        ec.edu.espe.zonas.entidades.TipoZona.class);
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException(
                        "roto", ife,
                        new org.springframework.mock.http.MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String mensaje = (String) response.getBody().get("mensaje");
        assertTrue(mensaje.contains("MORADO"));
        assertTrue(mensaje.contains("REGULAR"));
    }
}
