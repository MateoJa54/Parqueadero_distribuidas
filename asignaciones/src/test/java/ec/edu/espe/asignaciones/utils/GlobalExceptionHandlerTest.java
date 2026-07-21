package ec.edu.espe.asignaciones.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleNoEncontrado devuelve 404")
    void handleRecursoNoEncontrado() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No encontrado");
        ResponseEntity<Map<String, Object>> response = handler.handleNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("No encontrado", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleReglaNegocio devuelve 409")
    void handleReglaNegocio() {
        ReglaNegocioException ex = new ReglaNegocioException("Conflicto de negocio");
        ResponseEntity<Map<String, Object>> response = handler.handleReglaNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Conflicto de negocio", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleIntegridad devuelve 409 con mensaje fijo")
    void handleIntegridad() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint");
        ResponseEntity<Map<String, Object>> response = handler.handleIntegridad(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("El vehiculo ya tiene un propietario activo", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleRemote devuelve status del error HTTP remoto conocido")
    void handleRemoteKnownStatus() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "bad request", null, null, null);
        ResponseEntity<Map<String, Object>> response = handler.handleRemote(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error consultando un microservicio externo", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleRemote con status code no reconocido devuelve 502 BAD_GATEWAY")
    void handleRemoteUnrecognizedStatusCode() {
        // 999 is not a standard HTTP status — HttpStatus.resolve(999) returns null → fallback BAD_GATEWAY
        HttpServerErrorException ex = mock(HttpServerErrorException.class);
        when(ex.getStatusCode()).thenReturn(org.springframework.http.HttpStatusCode.valueOf(999));

        ResponseEntity<Map<String, Object>> response = handler.handleRemote(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().get("status"));
        assertEquals("Error consultando un microservicio externo", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleAccessDenied devuelve 403")
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("denied");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tiene permisos para esta operacion", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleRuntime devuelve 500 con mensaje")
    void handleRuntimeWithMessage() {
        RuntimeException ex = new RuntimeException("algo salio mal");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("algo salio mal", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleRuntime con mensaje null usa 'Error inesperado'")
    void handleRuntimeNullMessage() {
        RuntimeException ex = new RuntimeException((String) null);
        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error inesperado", response.getBody().get("mensaje"));
    }

    @Test
    @DisplayName("handleValidation devuelve 400 con mapa de errores de campo")
    void handleValidation() throws Exception {
        // Create a real MethodArgumentNotValidException with a field error
        Object target = new Object();
        BindingResult bindingResult = new BeanPropertyBindingResult(target, "req");
        bindingResult.addError(new FieldError("req", "userId", "El userId es obligatorio"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) response.getBody().get("errores");
        assertEquals("El userId es obligatorio", errores.get("userId"));
    }
}
