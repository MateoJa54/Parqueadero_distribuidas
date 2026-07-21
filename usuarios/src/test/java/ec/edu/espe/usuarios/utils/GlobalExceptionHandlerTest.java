package ec.edu.espe.usuarios.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleIllegalArgument retorna 400 con mensaje")
    void handleIllegalArgument() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleIllegalArgument(new IllegalArgumentException("bad param"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("bad param", resp.getBody().get("mensaje"));
        assertEquals(400, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("handleNoEncontrado retorna 404")
    void handleNoEncontrado() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNoEncontrado(new RecursoNoEncontradoException("no existe"));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("no existe", resp.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleReglaNegocio retorna 409")
    void handleReglaNegocio() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleReglaNegocio(new ReglaNegocioException("conflicto"));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("conflicto", resp.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleCredenciales retorna 401")
    void handleCredenciales() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleCredenciales(new CredencialesInvalidasException("cred invalidas"));

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("cred invalidas", resp.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleAccessDenied retorna 403 con mensaje generico")
    void handleAccessDenied() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertTrue(resp.getBody().get("mensaje").toString().contains("permisos"));
    }

    @Test
    @DisplayName("handleRuntime retorna 500 con mensaje de la excepcion")
    void handleRuntime() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleRuntime(new RuntimeException("error interno"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("error interno", resp.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleRuntime usa 'Error inesperado' si mensaje es null")
    void handleRuntimeNullMessage() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleRuntime(new RuntimeException((String) null));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("Error inesperado", resp.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("body contiene timestamp, status, error y mensaje")
    void bodyContieneTodasLasClaves() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNoEncontrado(new RecursoNoEncontradoException("x"));

        Map<String, Object> body = resp.getBody();
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("mensaje"));
    }
}
